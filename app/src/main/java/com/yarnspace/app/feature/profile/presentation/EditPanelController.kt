package com.yarnspace.app.feature.profile.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.yarnspace.app.R
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.core.theme.AccentColor
import com.yarnspace.app.core.theme.AvatarIcon

class EditPanelController(
    private val context: Context,
    private val view: View,
    private val viewModel: ProfileViewModel,
    private val onAccentPreview: (String) -> Unit,
    private val onSavePendingGlobalAccent: (String) -> Unit
) {

    private val editOverlay = view.findViewById<FrameLayout>(R.id.profileEditOverlay)
    private val btnEditClose = view.findViewById<ImageButton>(R.id.btnProfileEditClose)
    private val cvEditAvatarContainer = view.findViewById<MaterialCardView>(R.id.cvProfileEditAvatarContainer)
    private val ivEditAvatar = view.findViewById<ImageView>(R.id.ivProfileEditAvatar)
    private val llAvatarPickerContainer = view.findViewById<LinearLayout>(R.id.llAvatarPickerContainer)
    private val etEditDisplayName = view.findViewById<TextView>(R.id.etProfileEditDisplayName)
    private val accentGroup1 = view.findViewById<MaterialButtonToggleGroup>(R.id.profileEditAccentGroup)
    private val accentGroup2 = view.findViewById<MaterialButtonToggleGroup>(R.id.profileEditAccentGroup2)
    private val btnSave = view.findViewById<Button>(R.id.btnProfileEditSave)
    private val btnCancel = view.findViewById<Button>(R.id.btnProfileEditCancel)

    private val allAccentButtons = listOf(
        view.findViewById<MaterialButton>(R.id.btnAccentSage),
        view.findViewById<MaterialButton>(R.id.btnAccentPeach),
        view.findViewById<MaterialButton>(R.id.btnAccentLavender),
        view.findViewById<MaterialButton>(R.id.btnAccentYellow),
        view.findViewById<MaterialButton>(R.id.btnAccentPink),
        view.findViewById<MaterialButton>(R.id.btnAccentBlue)
    )

    private var initialDisplayName = ""
    private var initialAccent: String = AccentColor.SAGE.backendName
    private var currentAccent: String = initialAccent
    private var pickedAvatarIcon: String? = null

    init {
        btnEditClose.setOnClickListener { closePanel(force = false) }
        btnCancel.setOnClickListener { closePanel(force = false) }

        accentGroup1.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked || checkedId == View.NO_ID) return@addOnButtonCheckedListener
            accentGroup2.clearChecked()
            val name = group.findViewById<MaterialButton>(checkedId).tag as? String ?: return@addOnButtonCheckedListener
            onAccentSelected(name)
        }

        accentGroup2.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked || checkedId == View.NO_ID) return@addOnButtonCheckedListener
            accentGroup1.clearChecked()
            val name = group.findViewById<MaterialButton>(checkedId).tag as? String ?: return@addOnButtonCheckedListener
            onAccentSelected(name)
        }

        btnSave.setOnClickListener {
            val displayName = etEditDisplayName.text?.toString().orEmpty().trim()
            if (displayName.isBlank()) {
                Snackbar.make(view, context.getString(R.string.error_fill_all_fields), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onSavePendingGlobalAccent(currentAccent)

            setControlsEnabled(false)

            viewModel.saveMyProfile(
                displayName = displayName,
                accentColor = currentAccent,
                avatarIcon = pickedAvatarIcon,
            )
        }
    }

    fun open(profile: ProfilePublicDto) {
        initialDisplayName = profile.displayName.takeIf { !it.isNullOrBlank() } ?: profile.username
        initialAccent = AccentColor.fromBackendName(profile.accentColor).backendName

        etEditDisplayName.text = initialDisplayName

        pickedAvatarIcon = profile.avatarIcon
        val currentIconEnum = AvatarIcon.fromBackendName(pickedAvatarIcon)
        ivEditAvatar.setImageResource(currentIconEnum.resId)

        setupAvatarPicker()
        onAccentSelected(initialAccent)

        editOverlay.visibility = View.VISIBLE
    }

    fun isPanelOpen(): Boolean = editOverlay.visibility == View.VISIBLE

    fun closePanel(force: Boolean) {
        if (!force && isEditDirty()) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.profile_edit_discard_title))
                .setMessage(context.getString(R.string.profile_edit_discard_message))
                .setPositiveButton(context.getString(R.string.profile_edit_discard_action_discard)) { _, _ -> closePanel(force = true) }
                .setNegativeButton(context.getString(R.string.profile_edit_discard_action_keep), null)
                .show()
            return
        }
        editOverlay.visibility = View.GONE
        pickedAvatarIcon = null

        viewModel.uiState.value.profile?.let { onAccentPreview(it.accentColor) }
    }

    fun setControlsEnabled(enabled: Boolean) {
        btnSave.isEnabled = enabled
        btnCancel.isEnabled = enabled
        btnEditClose.isEnabled = enabled
    }

    private fun isEditDirty(): Boolean {
        val dn = etEditDisplayName.text?.toString().orEmpty().trim()
        return dn != initialDisplayName.trim() || currentAccent != initialAccent || pickedAvatarIcon != initialAvatar()
    }

    private fun initialAvatar(): String? = viewModel.uiState.value.profile?.avatarIcon

    private fun onAccentSelected(name: String) {
        currentAccent = name

        val target = allAccentButtons.firstOrNull { it.tag == name }
        if (target != null) {
            if (target.parent == accentGroup1 && accentGroup1.checkedButtonId != target.id) {
                accentGroup2.clearChecked()
                accentGroup1.check(target.id)
            } else if (target.parent == accentGroup2 && accentGroup2.checkedButtonId != target.id) {
                accentGroup1.clearChecked()
                accentGroup2.check(target.id)
            }
        }

        onAccentPreview(name)
        updateEditPanelUi(name)
    }

    private fun updateEditPanelUi(selectedAccentName: String) {
        val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val theme = AccentColor.fromBackendName(selectedAccentName)

        val primaryColor = ContextCompat.getColor(context, if (isNight) theme.nightColorResId else theme.colorResId)
        val colorStateList = ColorStateList.valueOf(primaryColor)

        allAccentButtons.forEach { btn ->
            val btnName = btn.tag as? String ?: ""
            if (btnName == selectedAccentName) { btn.alpha = 1.0f }
            else { btn.alpha = 0.35f }

            val btnTheme = AccentColor.fromBackendName(btnName)
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, if(isNight) btnTheme.nightColorResId else btnTheme.colorResId))
        }

        val bgTint = ContextCompat.getColor(context, theme.getLighterShade(isNight))
        val iconTint = ContextCompat.getColor(context, theme.getDarkerShade(isNight))

        for (i in 0 until llAvatarPickerContainer.childCount) {
            val card = llAvatarPickerContainer.getChildAt(i) as? MaterialCardView
            card?.setCardBackgroundColor(bgTint)
            val img = card?.getChildAt(0) as? ImageView
            img?.setColorFilter(iconTint)
        }

        cvEditAvatarContainer.setCardBackgroundColor(bgTint)
        ivEditAvatar.setColorFilter(iconTint)

        btnSave.backgroundTintList = colorStateList
        btnSave.setTextColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary))
    }

    private fun setupAvatarPicker() {
        llAvatarPickerContainer.removeAllViews()

        AvatarIcon.entries.forEach { iconEnum ->
            val cardView = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (64 * context.resources.displayMetrics.density).toInt(),
                    (64 * context.resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (12 * context.resources.displayMetrics.density).toInt()
                }
                radius = (32 * context.resources.displayMetrics.density)
                cardElevation = 0f
                strokeWidth = 0

                setOnClickListener {
                    pickedAvatarIcon = iconEnum.backendName
                    ivEditAvatar.setImageResource(iconEnum.resId)
                }
            }

            val imageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (32 * context.resources.displayMetrics.density).toInt(),
                    (32 * context.resources.displayMetrics.density).toInt()
                ).apply {
                    gravity = Gravity.CENTER
                }
                setImageResource(iconEnum.resId)
            }

            cardView.addView(imageView)
            llAvatarPickerContainer.addView(cardView)
        }
    }
}