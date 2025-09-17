package com.estudoapp.domain

import io.ktor.server.auth.*


data class UserPrincipal(val uid: String): Principal
