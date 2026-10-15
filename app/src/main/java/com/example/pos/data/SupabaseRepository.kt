package com.example.pos.data

// Delegating repository to SupabaseClient.service
class SupabaseRepository {
    val service: SupabaseService get() = SupabaseClient.service
}
