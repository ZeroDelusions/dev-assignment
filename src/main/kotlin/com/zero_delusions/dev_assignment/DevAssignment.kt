package com.zero_delusions.dev_assignment

import com.zero_delusions.dev_assignment.core.database.utils.HibernateUtils
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object DevAssignment : ModInitializer {
	const val MOD_ID = "dev-assignment"
    private val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		val sessionFactory = HibernateUtils.sessionFactory
		val session = sessionFactory.openSession()

		val count = session.createQuery("SELECT count(*) FROM UserData", Long::class.java).singleResult
		println("Found $count users")

		session.close()
	}
}