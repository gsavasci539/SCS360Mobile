package com.example.data.model

/** UI modes may narrow the real actor's roles, never impersonate another user. */
object SessionRolePolicy {
    fun primary(user: User): UserRole? {
        if (user.is_super_admin == true) return UserRole.SUPERADMIN
        if (user.roles.isNotEmpty() && user.roles.none { it.is_active }) return null
        val canonical = if (user.roles.isEmpty()) user.resolvedRole else user.copy(role = "").resolvedRole
        return UserRole.entries.firstOrNull { it.name == canonical && it != UserRole.SUPERADMIN }
    }

    fun canSelect(user: User, requested: UserRole): Boolean {
        if (requested == UserRole.SUPERADMIN) return user.is_super_admin == true
        if (user.roles.isEmpty()) return primary(user) == requested
        return user.roles.filter { it.is_active }.any {
            primary(User(roles = listOf(it))) == requested
        }
    }
}
