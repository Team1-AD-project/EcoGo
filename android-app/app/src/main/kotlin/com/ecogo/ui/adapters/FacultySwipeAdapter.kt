package com.ecogo.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.data.FacultyData
import com.ecogo.databinding.ItemFacultySwipeCardBinding

class FacultySwipeAdapter(
    private val faculties: List<FacultyData>,
    private val viewPager: androidx.viewpager2.widget.ViewPager2,
    private val onFacultySelected: (FacultyData) -> Unit
) : RecyclerView.Adapter<FacultySwipeAdapter.SwipeCardViewHolder>() {

    inner class SwipeCardViewHolder(private val binding: ItemFacultySwipeCardBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(faculty: FacultyData) {
            // Faculty info
            binding.textFacultyName.text = faculty.name
            binding.textFacultySlogan.text = faculty.slogan
            
            // Faculty color
            binding.viewFacultyColor.setBackgroundColor(Color.parseColor(faculty.color))
            
            // 卡片背景保持白色（已在XML中设置）
            
            // Mascot with outfit
            binding.mascotPreview.outfit = faculty.outfit
            
            // Outfit items list
            val outfitItems = mutableListOf<String>()
            if (faculty.outfit.head != "none") {
                outfitItems.add("• ${getItemName(faculty.outfit.head)}")
            }
            if (faculty.outfit.face != "none") {
                outfitItems.add("• ${getItemName(faculty.outfit.face)}")
            }
            if (faculty.outfit.body != "none") {
                outfitItems.add("• ${getItemName(faculty.outfit.body)}")
            }
            binding.textOutfitItems.text = outfitItems.joinToString("\n")
            
            // Click to select
            binding.cardFaculty.setOnClickListener {
                onFacultySelected(faculty)
            }
            
            // 左右切换按钮
            binding.btnPrev.setOnClickListener {
                val currentPosition = viewPager.currentItem
                if (currentPosition > 0) {
                    viewPager.setCurrentItem(currentPosition - 1, true)
                }
            }
            
            binding.btnNext.setOnClickListener {
                val currentPosition = viewPager.currentItem
                val itemCount = viewPager.adapter?.itemCount ?: 0
                if (currentPosition < itemCount - 1) {
                    viewPager.setCurrentItem(currentPosition + 1, true)
                }
            }
            
            // Add scale animation on touch
            binding.cardFaculty.setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        view.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .start()
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                }
                false // Let the click listener handle the event
            }
        }
        
        private fun getItemName(itemId: String): String {
            return when (itemId) {
                "hat_helmet" -> "Safety Helmet"
                "hat_beret" -> "Artist Beret"
                "hat_grad" -> "Grad Cap"
                "hat_cap" -> "Orange Cap"
                "face_goggles" -> "Safety Goggles"
                "glasses_sun" -> "Shades"
                "body_plaid" -> "Engin Plaid"
                "body_suit" -> "Biz Suit"
                "body_coat" -> "Lab Coat"
                "shirt_nus" -> "NUS Tee"
                "shirt_hoodie" -> "Blue Hoodie"
                else -> ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeCardViewHolder {
        val binding = ItemFacultySwipeCardBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return SwipeCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SwipeCardViewHolder, position: Int) {
        holder.bind(faculties[position])
    }

    override fun getItemCount() = faculties.size
}
