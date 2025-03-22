package com.zero_delusions.dev_assignment.core.database.utils

import com.zero_delusions.dev_assignment.core.database.table.UserData
import org.hibernate.Session

class InventoryQueryBuilder {
    enum class InventoryType(val columnName: String? = null) {
        MAIN("mainInventory"),
        ARMOUR("armourInventory"),
        ENDER_CHEST("enderChest"),
        OFF_HAND("offHandInventory"),
        ALL(null)
    }

    companion object {
        fun query(): Builder = Builder()
    }

    class Builder {
        private var itemId: String? = null
        private var minCount: Int = 0
        private var maxCount: Int = Int.MAX_VALUE
        private var inventoryType: InventoryType = InventoryType.ALL

        fun withItem(itemIdentifier: String): Builder = apply { this.itemId = itemIdentifier }
        fun withMinCount(minCount: Int): Builder = apply { this.minCount = minCount }
        fun withMaxCount(maxCount: Int): Builder = apply { this.maxCount = maxCount }
        fun inInventory(inventoryType: InventoryType): Builder = apply { this.inventoryType = inventoryType }

        fun execute(session: Session): List<UserData> {
            require(itemId != null && itemId!!.isNotEmpty()) { "Item identifier must be specified" }

            val hql = when(inventoryType) {
                InventoryType.ALL -> buildQueryForAllInventories(inventoryType.columnName!!)
                else -> buildQueryForInventory(inventoryType.columnName!!)
            }

            val escapedItemId = "\"$itemId\""

            val query = session.createQuery(hql, UserData::class.java)
            query.setParameter("itemId", escapedItemId)

            return query.resultList
        }

        private fun buildInventoryCondition(columnName: String): String {
            // We check if there exists at least some item in a specified inventory
            // that meets the requirements, by:
            // 1. Transforming JSON string into table
            // 2. For each item in that table (array), we perform action
            // 3. We put a whole object as item_data column
            // 4. Define the whole table as `items`
            // 5. For elements that aren't `null`, we perform min/max checks

            // (I hope I am correct on that)
            return """
                EXISTS (
                    SELECT 1 from JSON_TABLE(
                        u.$columnName->'$.values',
                        '$[*]' COLUMNS (item_data VARCHAR(255) PATH '$')
                    ) as items
                    WHERE items.item_data IS NOT NULL 
                    AND JSON_EXTRACT(items.item_data, '$.id') = :itemId
                    ${if (minCount > 0) "AND JSON_EXTRACT(items.item_data, '$.count') > $minCount " else ""}            
                    ${if (maxCount < Int.MAX_VALUE) "AND JSON_EXTRACT(items.item_data, '$.count') < $maxCount " else ""}            
                )
            """.trimIndent()
        }

        private fun buildQueryForAllInventories(columnName: String): String {
            return "FROM UserData u WHERE " + InventoryType.entries
                .filter { it != InventoryType.ALL }
                .joinToString(" OR ") { buildInventoryCondition(columnName) }
        }

        private fun buildQueryForInventory(columnName: String): String {
            return "FROM UserData u WHERE ${buildInventoryCondition(columnName)}"
        }
    }
}