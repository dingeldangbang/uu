package de.dingeldangbang.agentmobile

import android.app.Application
import androidx.room.Room
import de.dingeldangbang.agentmobile.agent.AgentRepository
import de.dingeldangbang.agentmobile.agent.LiteRtLocalEngine
import de.dingeldangbang.agentmobile.data.AgentDatabase
import de.dingeldangbang.agentmobile.network.CloudClient
import de.dingeldangbang.agentmobile.security.CredentialStore

class AgentApplication : Application() {
    lateinit var database: AgentDatabase
        private set
    lateinit var credentials: CredentialStore
        private set
    lateinit var cloudClient: CloudClient
        private set
    lateinit var localEngine: LiteRtLocalEngine
        private set
    lateinit var repository: AgentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AgentDatabase::class.java, "agent-mobile.db")
            .fallbackToDestructiveMigration()
            .build()
        credentials = CredentialStore(this)
        cloudClient = CloudClient(credentials)
        localEngine = LiteRtLocalEngine(this)
        repository = AgentRepository(database, credentials, cloudClient, localEngine, this)
    }
}
