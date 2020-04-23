package de.deftk.lonet.mobile.feature

import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.RemoteManageable
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.feature.abstract.IFilePrimitive
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.lonet.mobile.AuthStore
import java.util.*

@Deprecated("use own adapter")
class RootFileProvider : IFilePrimitive {

    override fun getFileStorageFiles(overwriteCache: Boolean): List<OnlineFile> {
        return listOf(
            createGroupRootFile(AuthStore.appUser),
            *AuthStore.appUser.getContext().getGroups()
                .filter { Feature.FILES.isAvailable(it.memberPermissions) }
                .map { createGroupRootFile(it) }.toTypedArray()
        )
    }

    override fun createFolder(name: String, description: String?): OnlineFile {
        error("Operation not supported!")
    }

    private fun createGroupRootFile(operator: AbstractOperator): OnlineFile {
        return OnlineFile(
            "/",
            operator.getLogin(),
            0,
            operator.getName(),
            "Root directory",
            OnlineFile.FileType.FOLDER,
            0,
            true,
            true, //TODO check permission
            false,
            false,
            Date(0),
            RemoteManageable("", "", -1, false),
            Date(0),
            RemoteManageable("", "", -1, false),
            operator
        )
    }

}