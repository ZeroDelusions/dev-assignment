package com.zero_delusions.dev_assignment

import com.zero_delusions.dev_assignment.core.database.service.InventoryService
import com.zero_delusions.dev_assignment.core.database.utils.HibernateUtils
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object DevAssignment : ModInitializer {
	const val MOD_ID = "dev-assignment"
    private val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		val users = InventoryService.findUsersWithDiamondChestplates(10)
		println("Users with 64 diamond chestplates: ${users.size}")
		users.forEach { user ->
			println("UUID: ${user.getJavaUUID()}")
		}
	}
}