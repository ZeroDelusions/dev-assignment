package com.zero_delusions.dev_assignment.core.database.builder;

import com.zero_delusions.dev_assignment.core.database.table.UserData;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class InventoryQueryBuilder {

    public enum InventoryType {
        MAIN("mainInventory"),
        ARMOUR("armourInventory"),
        ENDER_CHEST("enderChest"),
        OFF_HAND("offHandInventory"),
        ALL(null);

        private final String columnName;

        InventoryType(String columnName) {
            this.columnName = columnName;
        }

        public String getColumnName() {
            return columnName;
        }
    }

    public static Builder query() {
        return new Builder();
    }

    public static class Builder {
        private String itemId;
        private int minCount = 0;
        private int maxCount = Integer.MAX_VALUE;
        private InventoryType inventoryType = InventoryType.ALL;

        public Builder withItem(String itemIdentifier) {
            this.itemId = itemIdentifier;
            return this;
        }

        public Builder withMinCount(int minCount) {
            this.minCount = minCount;
            return this;
        }

        public Builder withMaxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder inInventory(InventoryType inventoryType) {
            this.inventoryType = inventoryType;
            return this;
        }

        public List<UserData> execute(Session session) {
            if (itemId == null || itemId.isEmpty()) {
                throw new IllegalArgumentException("Item identifier must be specified");
            }

            String hql = inventoryType == InventoryType.ALL
                    ? buildQueryForAllInventories()
                    : buildQueryForInventory(inventoryType.getColumnName());

            return session.createNativeQuery(hql, UserData.class)
                    .getResultList();
        }

        private String buildInventoryCondition(String columnName) {
            StringBuilder condition = new StringBuilder()
                    .append("EXISTS (")
                    .append("SELECT 1 FROM JSON_TABLE(")
                    .append("JSON_EXTRACT(u.").append(columnName).append(", '$.values'), '$[*]' ")
                    .append("COLUMNS (")
                    .append("item_data VARCHAR(512) PATH '$'")
                    .append(")) AS items ")
                    .append("WHERE items.item_data IS NOT NULL ")
                    .append("AND JSON_EXTRACT(items.item_data, '$.id') = '").append(itemId).append("' ");

            if (minCount > 0) {
                condition.append("AND JSON_EXTRACT(items.item_data, '$.count') >= ").append(minCount).append(" ");
            }
            if (maxCount < Integer.MAX_VALUE) {
                condition.append("AND JSON_EXTRACT(items.item_data, '$.count') <= ").append(maxCount).append(" ");
            }

            return condition.append(")").toString();
        }

        private String buildQueryForAllInventories() {
            String conditions = Arrays.stream(InventoryType.values())
                    .filter(type -> type != InventoryType.ALL)
                    .map(type -> buildInventoryCondition(type.getColumnName()))
                    .collect(Collectors.joining(" OR "));

            return "SELECT * FROM user_data u WHERE " + conditions;
        }

        private String buildQueryForInventory(String columnName) {
            return "SELECT * FROM user_data u WHERE " + buildInventoryCondition(columnName);
        }
    }
}