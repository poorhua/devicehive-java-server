package com.devicehive.dao.rdbms;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.*;
import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;

import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.devicehive.model.Device.Queries.Parameters.GUID;
import static java.util.Optional.ofNullable;

public class CriteriaHelper {

    /**
     * Creates an array of JPA predicates for networks list query. Add filter predicates for name and principal if required.
     * 1) if name is specified adds 'name = ?' predicate
     * 2) if name pattern is specified adds 'name like ?' predicate
     * 3) if principal is user of key without ADMIN role adds predicate for filtering not assigned networks
     * 4) if principal is key which has permissions only to specific networks adds 'network.id in (allowed_networks)' predicates
     *
     * @return array of above predicates
     * @see {com.devicehive.service.NetworkService#list(String, String, String, boolean, Integer, Integer, HivePrincipal)}
     */
    public static Predicate[] networkListPredicates(CriteriaBuilder cb, Root<Network> from, Optional<String> nameOpt, Optional<String> namePatternOpt, Optional<HivePrincipal> principalOpt) {
        List<Predicate> predicates = new LinkedList<>();

        nameOpt.ifPresent(name ->
                predicates.add(cb.equal(from.get("name"), name)));

        namePatternOpt.ifPresent(pattern ->
                predicates.add(cb.like(from.get("name"), pattern)));

        principalOpt.flatMap(principal -> {
            UserVO user = principal.getUser();

            return ofNullable(user);
        }).ifPresent(user -> {
            if (!user.isAdmin()) {
                User usr = User.convertToEntity(user);
                predicates.add(from.join("users").in(usr));
            }
        });

        principalOpt.flatMap(principal -> {
            Set<Long> networks = principal.getNetworks();

            return ofNullable(networks);
        }).ifPresent(networks -> {
            predicates.add(from.<Long>get("id").in(networks));
        });

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] networkPermissionsPredicates(CriteriaBuilder cb, Root<?> from, Set<AccessKeyPermissionVO> permissions) {
        List<Predicate> predicates = new ArrayList<>();
        for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices.createExtraFilters(permissions)) {
            List<Predicate> filter = new ArrayList<>();
            if (extraFilter.getNetworkIds() != null) {
                filter.add(from.get("id").in(extraFilter.getNetworkIds()));
            }
            predicates.add(cb.and(filter.toArray(new Predicate[filter.size()])));
        }
        return predicates.toArray(new Predicate[predicates.size()]);
    }

    /**
     * Adds ORDER BY ... ASC/DESC to query
     * Mutates provided criteria query
     * @param sortFieldOpt - field to sort by (field name in JPA Entity class)
     * @param asc - true if order should be ASC, false otherwise
     */
    public static void order(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<?> from, Optional<String> sortFieldOpt, boolean asc) {
        sortFieldOpt.ifPresent(sortField -> {
            Order order = asc ? cb.asc(from.get(sortField)) : cb.desc(from.get(sortField));
            cq.orderBy(order);
        });
    }

    public static Predicate[] userListPredicates(CriteriaBuilder cb, Root<User> from, Optional<String> loginOpt, Optional<String> loginPattern, Optional<Integer> roleOpt, Optional<Integer> statusOpt) {
        List<Predicate> predicates = new LinkedList<>();

        if (loginPattern.isPresent()) {
            loginPattern.ifPresent(pattern ->
                    predicates.add(cb.like(from.get("login"), pattern)));
        } else {
            loginOpt.ifPresent(login ->
                    predicates.add(cb.equal(from.get("login"), login)));
        }

        roleOpt.ifPresent(role -> predicates.add(cb.equal(from.get("role"), role)));
        statusOpt.ifPresent(status -> predicates.add(cb.equal(from.get("status"), status)));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] accessKeyListPredicates(CriteriaBuilder cb, Root<AccessKey> from, Long userId, Optional<String> labelOpt, Optional<String> labelPatten,
                                                      Optional<Integer> typeOpt) {
        List<Predicate> predicates = new LinkedList<>();

        Join user = (Join) from.fetch("user", JoinType.LEFT);
        predicates.add(cb.equal(user.get("id"), userId));

        if (labelPatten.isPresent()) {
            labelPatten.ifPresent(pattern -> predicates.add(cb.like(from.get("label"), pattern)));
        } else {
            labelOpt.ifPresent(label -> predicates.add(cb.equal(from.get("label"), label)));
        }

        typeOpt.ifPresent(type -> predicates.add(cb.equal(from.get("type"), type)));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] oAuthGrantsListPredicates(CriteriaBuilder cb, Root<OAuthGrant> from, UserVO user, Optional<Date> startOpt, Optional<Date> endOpt, Optional<String> oAuthIdOpt,
                                                        Optional<Integer> typeOpt, Optional<String> scopeOpt, Optional<String> redirectUri, Optional<Integer> accessType) {
        List<Predicate> predicates = new LinkedList<>();

        if (!user.isAdmin()) {
            User u = User.convertToEntity(user);
            predicates.add(from.join("user").in(u));
        }

        startOpt.ifPresent(start -> predicates.add(cb.greaterThan(from.get("timestamp"), start)));
        endOpt.ifPresent(end -> predicates.add(cb.lessThan(from.get("timestamp"), end)));
        oAuthIdOpt.ifPresent(id -> predicates.add(cb.equal(from.join("client").get("oauthId"), id)));
        typeOpt.ifPresent(type -> predicates.add(cb.equal(from.get("type"), type)));
        scopeOpt.ifPresent(scope -> predicates.add(cb.equal(from.get("scope"), scope)));
        redirectUri.ifPresent(uri -> predicates.add(cb.equal(from.get("redirectUri"), uri)));
        accessType.ifPresent(at -> predicates.add(cb.equal(from.get("accessType"), at)));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] oAuthClientListPredicates(CriteriaBuilder cb, Root<OAuthClient> from, Optional<String> nameOpt, Optional<String> namePattern, Optional<String> domainOpt,
                                                        Optional<String> oauthIdOpt) {
        List<Predicate> predicates = new LinkedList<>();

        if (namePattern.isPresent()) {
            namePattern.ifPresent(pattern -> predicates.add(cb.like(from.get("name"), pattern)));
        } else {
            nameOpt.ifPresent(name -> predicates.add(cb.equal(from.get("name"), name)));
        }
        domainOpt.ifPresent(domain -> predicates.add(cb.equal(from.get("domain"), domain)));
        oauthIdOpt.ifPresent(id -> predicates.add(cb.equal(from.get("oauthId"), id)));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] deviceListPredicates(CriteriaBuilder cb,
                                                   Root<Device> from,
                                                   List<String> guids,
                                                   Optional<HivePrincipal> principal) {
        final List<Predicate> predicates = deviceSpecificPrincipalPredicates(cb, from, principal);
        if (guids != null && !guids.isEmpty()) {
            predicates.add(from.get(GUID).in(guids));
        }

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    @SuppressWarnings("unchecked")
    public static Predicate[] deviceListPredicates(CriteriaBuilder cb,
                                                   Root<Device> from,
                                                   Optional<String> name,
                                                   Optional<String> namePattern,
                                                   Optional<String> status,
                                                   Optional<Long> networkId,
                                                   Optional<String> networkName,
                                                   Optional<Long> deviceClassId,
                                                   Optional<String> deviceClassName,
                                                   Optional<HivePrincipal> principal) {
        final List<Predicate> predicates = new LinkedList<>();

        name.ifPresent(n -> predicates.add(cb.equal(from.<String>get("name"), n)));
        namePattern.ifPresent(np -> predicates.add(cb.like(from.<String>get("name"), np)));
        status.ifPresent(s -> predicates.add(cb.equal(from.<String>get("status"), s)));

        final Join<Device, Network> networkJoin = (Join) from.fetch("network", JoinType.LEFT);
        networkId.ifPresent(nId -> predicates.add(cb.equal(networkJoin.<Long>get("id"), nId)));
        networkName.ifPresent(nName ->  predicates.add(cb.equal(networkJoin.<String>get("name"), nName)));

        final Join<Device, DeviceClass> dcJoin = (Join) from.fetch("deviceClass", JoinType.LEFT);
        deviceClassId.ifPresent(dcId -> predicates.add(cb.equal(dcJoin.<Long>get("id"), dcId)));
        deviceClassName.ifPresent(dcName -> predicates.add(cb.equal(dcJoin.<String>get("name"), dcName)));

        predicates.addAll(deviceSpecificPrincipalPredicates(cb, from, principal));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] deviceClassListPredicates(CriteriaBuilder cb, Root<DeviceClass> from, Optional<String> name,
                                                 Optional<String>  namePattern) {
        final List<Predicate> predicates = new LinkedList<>();
        if (namePattern.isPresent()) {
            namePattern.ifPresent(np -> predicates.add(cb.like(from.get("name"), np)));
        } else {
            name.ifPresent(n -> predicates.add(cb.equal(from.get("name"), n)));
        }

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    @SuppressWarnings("unchecked")
    private static List<Predicate> deviceSpecificPrincipalPredicates(CriteriaBuilder cb, Root<Device> from, Optional<HivePrincipal> principal) {
        final List<Predicate> predicates = new LinkedList<>();
        final Join<Device, Network> networkJoin = (Join) from.fetch("network", JoinType.LEFT);
        final Join<Device, Network> usersJoin = (Join) networkJoin.fetch("users", JoinType.LEFT);
        from.fetch("deviceClass", JoinType.LEFT); //need this fetch to populate deviceClass
        principal.ifPresent(p -> {
            UserVO user = p.getUser();

            if (user != null && !user.isAdmin()) {
                predicates.add(cb.equal(usersJoin.<Long>get("id"), user.getId()));
            }

            if (p.getNetworks() != null) {
                predicates.add(networkJoin.<Long>get("id").in(p.getNetworks()));
            }

            if (p.getDevices() != null) {
                predicates.add(from.<String>get("guid").in(p.getDevices()));
            }
        });

        return predicates;
    }
}
