package de.deftk.openlonet.api

@Deprecated("replace with sealed Response class")
enum class ApiState {
    LOADING,
    SUCCESS,
    ERROR
}