package com.devicehive.dao.rdbms;


import com.devicehive.dao.AccessKeyPermissionDao;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Profile({"rdbms"})
@Repository
public class AccessKeyPermissionDaoRdbmsImpl extends RdbmsGenericDao implements AccessKeyPermissionDao {

    @Override
    public int deleteByAccessKey(AccessKey key) {
        return createNamedQuery("AccessKeyPermission.deleteByAccessKey", Optional.<CacheConfig>empty())
                .setParameter("accessKey", key)
                .executeUpdate();
    }

    @Override
    public void persist(AccessKey key, AccessKeyPermission accessKeyPermission) {
        super.persist(accessKeyPermission);
    }

    @Override
    public AccessKeyPermission merge(AccessKeyPermission existing) {
        return super.merge(existing);
    }
}
