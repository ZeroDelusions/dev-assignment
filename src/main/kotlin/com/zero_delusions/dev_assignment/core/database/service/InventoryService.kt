package com.zero_delusions.dev_assignment.core.database.service

import com.zero_delusions.dev_assignment.core.database.builder.InventoryQueryBuilder
import com.zero_delusions.dev_assignment.core.database.table.UserData
import com.zero_delusions.dev_assignment.core.database.utils.HibernateUtils
import org.hibernate.Session

object InventoryService {
    fun findUsersWithDiamondChestplates(minCount: Int): List<UserData> {
        return HibernateUtils.executeWithSession { session: Session ->
            InventoryQueryBuilder.query()
                .withItem("minecraft:diamond_chestplate")
                .withMinCount(64)
                .execute(session)
        }
    }
}