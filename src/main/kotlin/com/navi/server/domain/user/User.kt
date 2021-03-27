package com.navi.server.domain.user

import com.navi.server.domain.FileObject
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.stream.Collectors
import javax.persistence.Id

@Document(collection="user")
class User(
    @Id
    var id: ObjectId = ObjectId(),
    var userName: String = "",
    var userEmail: String = "",
    var userPassword: String = "",
    val roles: Set<String> = setOf(),
    val fileList: MutableList<FileObject> = mutableListOf()
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority?>? {
        return roles.stream()
            .map { role: String? ->
                SimpleGrantedAuthority(
                    role
                )
            }
            .collect(Collectors.toList())
    }

    override fun getPassword() = userPassword
    override fun getUsername(): String? = userName
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}