package com.zero_delusions.dev_assignment;

import com.zero_delusions.dev_assignment.core.command.DevAssignmentCommands;
import com.zero_delusions.dev_assignment.core.database.service.InventoryService;
import com.zero_delusions.dev_assignment.core.database.table.UserData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

final class DevAssignment implements ModInitializer {
    final String MOD_ID = "dev-assignment";
    Logger logger = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        List<UserData> users = InventoryService.findUsersWithDiamondChestplates(64);
        System.out.println("Users with 64 diamond chestplates: " + users.size());
        users.forEach(user ->
                System.out.println("UUID: " + user.getJavaUUID())
        );

        // Register commands
        CommandRegistrationCallback.EVENT.register(DevAssignmentCommands::register);
    }
}