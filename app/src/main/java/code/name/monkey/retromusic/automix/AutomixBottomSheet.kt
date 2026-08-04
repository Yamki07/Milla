/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import code.name.monkey.retromusic.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView

/**
 * Panel de Control de Mezcla Manual (Bottom Sheet) que permite al usuario o DJ
 * ajustar el tiempo de crossfade, la curva de transición y el Beatmatching armónico.
 */
class AutomixBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_automix_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val slider = view.findViewById<Slider>(R.id.crossfadeSlider)
        val valueText = view.findViewById<MaterialTextView>(R.id.crossfadeValueText)
        val radioGroup = view.findViewById<RadioGroup>(R.id.curveRadioGroup)
        val beatmatchSwitch = view.findViewById<SwitchMaterial>(R.id.beatmatchSwitch)
        val applyButton = view.findViewById<MaterialButton>(R.id.applyButton)

        val engine = AutomixPlayerEngine.getInstance(requireContext())
        slider.value = (engine.manualCrossfadeDurationMs / 1000L).toFloat()
        valueText.text = "${slider.value.toInt()} segundos"
        beatmatchSwitch.isChecked = engine.isBeatmatchEnabled

        when (engine.transitionCurveMode) {
            "HIGH_ENERGY" -> radioGroup.check(R.id.radioHighEnergy)
            "HARMONIC" -> radioGroup.check(R.id.radioHarmonic)
            "EQUAL_POWER" -> radioGroup.check(R.id.radioEqualPower)
            else -> radioGroup.check(R.id.radioAuto)
        }

        slider.addOnChangeListener { _, value, _ ->
            valueText.text = "${value.toInt()} segundos"
        }

        applyButton.setOnClickListener {
            val selectedCurve = when (radioGroup.checkedRadioButtonId) {
                R.id.radioHighEnergy -> "HIGH_ENERGY"
                R.id.radioHarmonic -> "HARMONIC"
                R.id.radioEqualPower -> "EQUAL_POWER"
                else -> "AUTO_IA"
            }

            engine.manualCrossfadeDurationMs = (slider.value.toLong() * 1000L)
            engine.transitionCurveMode = selectedCurve
            engine.isBeatmatchEnabled = beatmatchSwitch.isChecked

            Toast.makeText(requireContext(), "Parámetros DJ aplicados", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    companion object {
        const val TAG = "AutomixBottomSheet"

        fun newInstance(): AutomixBottomSheet {
            return AutomixBottomSheet()
        }
    }
}
