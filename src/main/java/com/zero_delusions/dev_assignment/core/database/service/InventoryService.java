package com.zero_delusions.dev_assignment.core.database.service;

import com.zero_delusions.dev_assignment.core.database.builder.InventoryQueryBuilder;
import com.zero_delusions.dev_assignment.core.database.table.UserData;
import com.zero_delusions.dev_assignment.core.database.utils.HibernateUtils;
import org.hibernate.Session;

import java.util.List;

public final class InventoryService {
    public static List<UserData> findUsersWithDiamondChestplates(int minCount) {
        return HibernateUtils.executeWithSession((Session session) ->
                InventoryQueryBuilder.query()
                        .withItem("minecraft:diamond_chestplate")
                        .withMinCount(minCount)
                        .inInventory(InventoryQueryBuilder.InventoryType.ARMOUR)
                        .execute(session)
        );
    }
}