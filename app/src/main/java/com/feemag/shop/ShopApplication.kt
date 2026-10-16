package com.feemag.shop

import android.app.Application
import android.content.Context

class ShopApplication : Application() {
 val repository by lazy { ShopRepository(PreferencesCartStorage(this)) }
}

private class PreferencesCartStorage(context: Context) : CartStorage {
 private val preferences = context.getSharedPreferences("demo_cart", Context.MODE_PRIVATE)
 override fun load(): Map<String, Int> = preferences.all.mapNotNull { (id, value) ->
  (value as? Int)?.let { id to it }
 }.toMap()
 override fun save(quantities: Map<String, Int>) {
  val editor = preferences.edit().clear()
  quantities.forEach { (id, quantity) -> editor.putInt(id, quantity) }
  check(editor.commit()) { "Could not save the cart. Please retry." }
 }
}
