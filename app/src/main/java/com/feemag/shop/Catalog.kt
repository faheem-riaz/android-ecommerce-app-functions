package com.feemag.shop

object Catalog {
 val categories = listOf(Category("audio", "Audio"), Category("workspace", "Workspace"), Category("everyday", "Everyday"))
 val products = listOf(
  Product("audio-01", "Studio Wireless Headphones", "Over-ear wireless headphones with noise cancellation and 40-hour battery.", "audio", 7990, "EUR", 12),
  Product("audio-02", "Pocket Bluetooth Speaker", "Portable water-resistant speaker for music on the move.", "audio", 3490, "EUR", 20),
  Product("audio-03", "Everyday Earbuds", "Compact wireless earbuds with charging case and clear voice calls.", "audio", 4990, "EUR", 0),
  Product("desk-01", "Focus Desk Lamp", "Adjustable warm LED desk lamp with three brightness levels.", "workspace", 4590, "EUR", 8),
  Product("desk-02", "Mechanical Keyboard", "Compact keyboard with tactile switches and USB-C connection.", "workspace", 8990, "EUR", 6),
  Product("desk-03", "Laptop Stand", "Foldable aluminium laptop stand for a comfortable workspace.", "workspace", 2990, "EUR", 15),
  Product("desk-04", "Felt Desk Mat", "Soft recycled felt desk mat, 80 by 40 centimetres.", "workspace", 1990, "EUR", 25),
  Product("daily-01", "Daypack 18L", "Lightweight everyday backpack with padded laptop compartment.", "everyday", 5990, "EUR", 9),
  Product("daily-02", "Insulated Bottle", "Reusable 600 ml stainless steel water bottle.", "everyday", 2490, "EUR", 30),
  Product("daily-03", "Pocket Notebook", "A5 dotted notebook with 160 pages for daily ideas.", "everyday", 990, "EUR", 40),
  Product("daily-04", "Travel Organizer", "Compact pouch for cables, adapters and small essentials.", "everyday", 1590, "EUR", 18),
  Product("daily-05", "Canvas Tote", "Durable cotton shopping bag with an inside pocket.", "everyday", 1290, "EUR", 22),
 )
}
