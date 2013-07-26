package com.devicehive.controller;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.json.strategies.JsonPolicyDef.Policy;
import com.devicehive.messages.util.Params;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author rroschin
 */
@Path("/device")
public class DeviceNotificationController {

    @Inject
    private DeviceNotificationDAO notificationDAO;
    @Inject
    private DeviceDAO deviceDAO;

    @GET
    @Path("/{deviceGuid}/notification")
    @RolesAllowed({"CLIENT", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT)
    public List<DeviceNotification> query(@PathParam("deviceGuid") String guid,
                                          @QueryParam("start") String start,
                                          @QueryParam("end") String end,
                                          @QueryParam("notification") String notification,
                                          @QueryParam("sortField") String sortField,
                                          @QueryParam("sortOrder") String sortOrder,
                                          @QueryParam("take") Integer take,
                                          @QueryParam("skip") Integer skip) {
        if (sortOrder != null && !sortOrder.equals("DESC") && !sortOrder.equals("ASC")) {
            throw new BadRequestException("The sort order cannot be equal " + sortOrder);
        }
        boolean sortOrderAsc = true;
        if ("DESC".equals(sortOrder)) {
            sortOrderAsc = false;
        }
        if (!"Timestamp".equals(sortField) && !"Notification".equals(sortField) && sortField != null) {
            throw new BadRequestException("The sort field cannot be equal " + sortField);
        }
        if (sortField == null) {
            sortField = "timestamp";
        }
        sortField = sortField.toLowerCase();

        Date startTimestamp = null, endTimestamp = null;

        if (start != null) {
            startTimestamp = Params.parseUTCDate(start);
            if (startTimestamp == null) {
                throw new BadRequestException("unparseable date " + start);
            }
        }
        if (end != null) {
            endTimestamp = Params.parseUTCDate(end);
            if (endTimestamp == null) {
                throw new BadRequestException("unparseable date " + end);
            }
        }

        Device device = getDevice(guid);
        return notificationDAO.queryDeviceNotification(device, startTimestamp, endTimestamp, notification, sortField,
                sortOrderAsc, take, skip);
    }

    @GET
    @Path("/{deviceGuid}/notification/{id}")
    @RolesAllowed({"CLIENT", "ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT)
    public DeviceNotification get(@PathParam("deviceGuid") String guid, @PathParam("id") Long notificationId) {
        DeviceNotification deviceNotification = notificationDAO.findById(notificationId);
        String deviceGuidFromNotification = deviceNotification.getDevice().getGuid().toString();
        if (!deviceGuidFromNotification.equals(guid)) {
            throw new NotFoundException("Notification with id: " + notificationId + " associated with device: " +
                    guid + " not found");
        }
        return deviceNotification;
    }

    private Device getDevice(String uuid) {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unparseable guid: " + uuid);
        }
        Device device = deviceDAO.findByUUID(deviceId);
        if (device == null) {
            throw new NotFoundException("device with guid " + uuid + " not found");
        }
        return device;
    }

    @GET
    @RolesAllowed({"CLIENT", "DEVICE", "ADMIN"})
    @Path("/{deviceGuid}/notification/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(Policy.COMMAND_TO_DEVICE)
    public List<DeviceCommand> poll(
            @PathParam("deviceGuid") String deviceGuid,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout) {

        return null;
    }

    @GET
    @RolesAllowed({"CLIENT", "DEVICE", "ADMIN"})
    @Path("/notification/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPolicyApply(Policy.COMMAND_TO_DEVICE)
    public List<DeviceCommand> pollMany(
            @QueryParam("deviceGuids") String deviceGuids,
            @QueryParam("timestamp") String timestampUTC,
            @QueryParam("waitTimeout") String waitTimeout) {

        return null;
    }
}
