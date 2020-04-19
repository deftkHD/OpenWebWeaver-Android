package de.deftk.lonet.mobile.feature

import com.google.gson.Gson
import com.google.gson.JsonObject
import de.deftk.lonet.api.model.Member
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.files.FileProvider
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.lonet.mobile.AuthStore

//TODO should maybe be part of the api-wrapper...

// since I overwrite the only function that needs these, I can simply ignore them
class RootFileProvider : FileProvider("", null, "") {

    companion object {
        private val gson = Gson()
    }

    override fun getFiles(sessionId: String, overwriteCache: Boolean): List<OnlineFile> {
        return listOf(
            createMemberRootFile(AuthStore.appUser),
            *AuthStore.appUser.memberships
                .filter { it.memberPermissions.contains(Permission.FILES) || it.memberPermissions.contains(Permission.FILES_ADMIN) || it.memberPermissions.contains(Permission.FILES_WRITE) }
                .map { createMemberRootFile(it) }.toTypedArray()
        )
    }

    private fun createMemberRootFile(member: Member): OnlineFile {
        //TODO not sure about if files_admin also is important
        // but generally its not nice practice to do it like this (OnlineFile should have a custom
        // constructor for all properties instead of giving it just json (add static createFromJson() function))

        // cache file quota for later. yep, that't not good practise :/
        val quota = member.getFileQuota(AuthStore.appUser.sessionId, true)

        val json = """
            {
                "id": "/",
                "parent_id": "${member.login}",
                "name": "${member.name!!}",
                "ordinal": 0,
                "description": "Root directory",
                "type": "folder",
                "size": 0,
                "readable": 1,
                "writable": ${if (member.memberPermissions.contains(Permission.FILES_WRITE)) "1" else "0"},
                "sparse": 0,
                "mine": 0,
                "created": {
                    "date": 0,
                    "user": {
                        "login": "",
                        "name_hr": "",
                        "type": -1
                    }
                },
                "modified": {
                    "date": 0,
                    "user": {
                        "login": "",
                        "name_hr": "",
                        "type": 0
                    }
                }
                
                
            }
        """.trimIndent()
        return OnlineFile(
            gson.fromJson(json, JsonObject::class.java),
            member.responsibleHost,
            member.login
        )
    }

}