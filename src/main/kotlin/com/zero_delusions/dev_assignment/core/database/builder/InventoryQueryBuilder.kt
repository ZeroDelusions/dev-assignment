package com.zero_delusions.dev_assignment.core.database.builder

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
                InventoryType.ALL -> buildQueryForAllInventories()
                else -> buildQueryForInventory(inventoryType.columnName!!)
            }

            val query = session.createNativeQuery(hql, UserData::class.java)

            return query.resultList
        }

        private fun buildInventoryCondition(columnName: String): String {
            return """
                EXISTS (
                    SELECT 1 FROM JSON_TABLE(
                        JSON_EXTRACT(u.$columnName, '$.values'), '$[*]'
                        COLUMNS (
                            item_data VARCHAR(512) PATH '$'
                        )
                    ) AS items
                    WHERE items.item_data IS NOT NULL 
                      AND JSON_EXTRACT(items.item_data, '$.id') = '${this.itemId}'
                      ${if (minCount > 0) "AND JSON_EXTRACT(JSON_UNQUOTE(items.item_data), '$.count') >= $minCount " else ""}
                      ${if (maxCount < Int.MAX_VALUE) "AND JSON_EXTRACT(JSON_UNQUOTE(items.item_data), '$.count') <= $maxCount " else ""}
                )
            """.trimIndent()
        }

        private fun buildQueryForAllInventories(): String {
            return "SELECT * FROM user_data u WHERE " + InventoryType.entries
                .filter { it != InventoryType.ALL }
                .joinToString(" OR ") { buildInventoryCondition(it.columnName!!) }
        }

        private fun buildQueryForInventory(columnName: String): String {
            return "SELECT * FROM user_data u WHERE ${buildInventoryCondition(columnName)}"
        }
    }
}