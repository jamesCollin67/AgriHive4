package com.example.agrihive.hivestreams

/**
 * Shared diagnosis text and severity styling for scan results and saved diagnosis detail.
 */
object DiagnosisCopy {

    fun severityBadge(score: Int): Triple<String, String, String> {
        return when {
            score < 50 -> Triple("Severity: High", "#5C1F1F", "#EF5350")
            score < 75 -> Triple("Severity: Moderate", "#4A3820", "#FF9800")
            else -> Triple("Severity: Low", "#1F3D2A", "#66BB6A")
        }
    }

    fun symptomsFor(diseaseDisplayName: String): String {
        return when (diseaseDisplayName) {
            "Healthy Colony" ->
                "• Active bees with steady hum\n• Clear entrance activity\n• Healthy brood patterns\n• Bees bringing in pollen"
            "Varroa Mite Infestation" ->
                "• Deformed wings on adult bees\n• Visible mites on bees or brood\n• Reduced bee population\n• Spotty brood pattern"
            "Chalkbrood Infection" ->
                "• White/gray mummified larvae in brood cells\n• Irregular brood pattern\n• Reduced brood viability\n• Fungal growth in damp/cool hive areas"
            "Dead Bees / Colony Loss" ->
                "• Pile of dead bees at entrance or floor\n• No activity during warm weather\n• Cold hive with no vibration\n• Intact food stores but dead bees"
            "Not a bee" ->
                "• AI did not detect honey bees in frame\n• Subject may be another object or poor framing\n• Image may be out of focus"
            else ->
                "• AI did not detect a specific bee condition\n• Image might be blurry or far away\n• Non-bee object in focus"
        }
    }

    fun treatmentsFor(diseaseDisplayName: String): String {
        return when (diseaseDisplayName) {
            "Healthy Colony" ->
                "1. Regular monitoring monthly\n2. Ensure fresh water source nearby\n3. Check for mites every 30 days\n4. Maintain enough food stores"
            "Varroa Mite Infestation" ->
                "1. Formic acid application\n2. Oxalic acid treatment\n3. Thymol strips (Apiguard)\n4. Screened bottom boards\n5. Brood interruption method\n6. Regular mite monitoring"
            "Chalkbrood Infection" ->
                "1. Improve hive ventilation and reduce moisture\n2. Replace old combs and clean affected frames\n3. Strengthen colony with good nutrition\n4. Re-queen if colony remains weak"
            "Dead Bees / Colony Loss" ->
                "1. Clean out the hive and debris\n2. Investigate for starvation or disease\n3. Sterilize equipment before reuse\n4. Seal hive to prevent robbing"
            "Not a bee" ->
                "1. Ensure good lighting and focus\n2. Crop image closer to the bees\n3. Clean camera lens\n4. Try capturing a different angle"
            else ->
                "1. Ensure good lighting and focus\n2. Crop image closer to the bees\n3. Clean camera lens\n4. Try capturing a different angle"
        }
    }
}
