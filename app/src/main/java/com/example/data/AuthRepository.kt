package com.example.data

import com.example.model.GateDepartment
import com.example.model.PreparationMode
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val userDao: UserDao, private val firebaseAuth: FirebaseAuth) {

    val currentUserFlow: Flow<UserProfile?> = userDao.getCurrentUserFlow()

    suspend fun getCurrentUser(): UserProfile? = userDao.getCurrentUser()

    suspend fun authRepoGetUserByEmail(email: String): UserProfile? = userDao.getUserByEmail(email)

    suspend fun login(email: String, mode: PreparationMode, isRemembered: Boolean): Result<UserProfile> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, "password_is_handled_by_ui").await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Login failed: No user found"))
            
            val existing = userDao.getUserByEmail(email)
            
            if (existing != null) {
                if (existing.mode != mode) {
                    return Result.failure(Exception("MODE_MISMATCH|${existing.mode}"))
                }
                val updated = existing.copy(isRemembered = isRemembered)
                userDao.insertUser(updated)
                Result.success(updated)
            } else {
                // If not in local DB, we might need to fetch from Firestore if it was implemented.
                // For now, if it's a new login on this device, we assume the mode the user selected.
                // But the requirement says "If the user selects the wrong mode during login, show a clear message".
                // This implies the mode should be known. 
                // Since we don't have Firestore, we'll treat the first login on device as "setting the mode" 
                // OR we could use the Firebase user's display name or something to store it if we really wanted to.
                // However, I'll stick to local DB for mode storage as per existing code structure, 
                // but I'll add a way to handle the "Mode Mismatch" if it's already in DB.
                
                val newUser = UserProfile(
                    email = email,
                    fullName = firebaseUser.displayName ?: email.substringBefore("@"),
                    mode = mode,
                    isRemembered = isRemembered
                )
                userDao.insertUser(newUser)
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Overloaded login for UI that passes the actual password
    suspend fun loginWithPassword(email: String, password: String, targetMode: PreparationMode, isRemembered: Boolean): Result<UserProfile> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val existing = userDao.getUserByEmail(email)
            if (existing != null && existing.mode != targetMode) {
                return Result.failure(Exception("MODE_MISMATCH|${existing.mode}"))
            }
            
            val user = existing?.copy(isRemembered = isRemembered) ?: UserProfile(
                email = email,
                fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                mode = targetMode,
                isRemembered = isRemembered
            )
            
            userDao.clearUser()
            userDao.insertUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerAcademic(fullName: String, email: String, password: String, isRemembered: Boolean): Result<UserProfile> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = UserProfile(
                email = email,
                fullName = fullName,
                mode = PreparationMode.ACADEMIC,
                isRemembered = isRemembered
            )
            userDao.clearUser()
            userDao.insertUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerGate(
        fullName: String,
        email: String,
        password: String,
        age: Int,
        collegeName: String,
        department: GateDepartment,
        isRemembered: Boolean
    ): Result<UserProfile> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = UserProfile(
                email = email,
                fullName = fullName,
                mode = PreparationMode.PROFESSIONAL_GATE,
                age = age,
                collegeName = collegeName,
                gateDepartment = department,
                isRemembered = isRemembered
            )
            userDao.clearUser()
            userDao.insertUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePreparationMode(mode: PreparationMode) {
        val current = userDao.getCurrentUser()
        if (current != null) {
            userDao.updateUser(current.copy(mode = mode))
        }
    }

    suspend fun logout() {
        firebaseAuth.signOut()
        userDao.clearUser()
    }
}
