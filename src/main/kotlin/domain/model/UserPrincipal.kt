package com.estudoapp.domain.model

import io.ktor.server.auth.*


data class UserPrincipal(val uid: String): Principal
