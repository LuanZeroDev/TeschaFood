package com.tescha.food.di

import android.content.Context
import com.tescha.food.data.local.SessionManager
import com.tescha.food.data.repository.OrderRepository
import com.tescha.food.data.repository.ProductRepository
import com.tescha.food.data.repository.UserRepository
import com.tescha.food.data.repository.supabase.SupabaseOrderRepository
import com.tescha.food.data.repository.supabase.SupabaseProductRepository
import com.tescha.food.data.repository.supabase.SupabaseUserRepository
import com.tescha.food.service.LocationService

class AppContainer(context: Context) {
    val userRepository: UserRepository = SupabaseUserRepository()
    val productRepository: ProductRepository = SupabaseProductRepository()
    val orderRepository: OrderRepository = SupabaseOrderRepository()
    val sessionManager: SessionManager = SessionManager(context)
    val locationService: LocationService = LocationService(context)
}
