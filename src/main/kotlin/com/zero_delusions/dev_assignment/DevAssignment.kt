package com.zero_delusions.dev_assignment

import com.zero_delusions.dev_assignment.core.command.DevAssignmentCommands
import com.zero_delusions.dev_assignment.core.database.service.InventoryService
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.LoggerFactory

object DevAssignment : ModInitializer {
	const val MOD_ID = "dev-assignment"
    private val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		val users = InventoryService.findUsersWithDiamondChestplates(64)
		println("Users with 64 diamond chestplates: ${users.size}")
		users.forEach { user ->
			println("UUID: ${user.getJavaUUID()}")
		}

		// Register commands
		CommandRegistrationCallback.EVENT.register(DevAssignmentCommands::register)
	}
}