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
                "1. Confirm the infestation first. Do not treat based on appearance alone. Use a mite test, usually an alcohol wash or powdered sugar roll, to estimate the mite level in the colony.\n" +
                "2. Check if the colony already needs treatment. A conservative target is to keep mite levels below 1% year-round, and treatment is commonly warranted when levels go above 2%. Also remember that testing adult bees can underestimate the true mite load when the colony has a lot of brood.\n" +
                "3. Assess the season and brood condition of the hive. This matters because treatment choice changes depending on whether the colony has capped brood or is relatively broodless. Varroa populations can rebound quickly, and in colonies with brood, they can increase very fast, so timing matters.\n" +
                "4. Choose the treatment based on brood status.\n" +
                "- If the colony is broodless or has very little brood, oxalic acid is often used because it works best when mites are on adult bees and not protected inside capped cells.\n" +
                "- If the colony has capped brood, formic acid is often preferred because it is the soft acaricide specifically noted to penetrate capped brood.\n" +
                "- Other registered treatment categories include thymol, amitraz, fluvalinate, and newer approved products in some countries, but which one is appropriate depends on local registration, season, temperature, and resistance risk.\n" +
                "5. Use only registered products and follow the label exactly. Do not use unregistered chemicals or use a registered product outside its label conditions.\n" +
                "6. Treat as part of an IPM plan, not as a one-time fix. Regular monitoring, cultural practices such as drone brood removal, requeening with varroa-resistant stock when possible.\n" +
                "7. Reduce reinfestation pressure while treating. Highly infested colonies can spread mites and associated disease to other colonies through drifting, robbing, swarming, and the movement of bees.\n" +
                "8. Re-test after treatment.\n" +
                "9. Keep records and rotate actives for the next cycle."
            "Chalkbrood Infection" ->
                "1. Confirm that it is chalkbrood.\n" +
                "2. Do not rely on chemical medicine as the main treatment.\n" +
                "3. Prevent the disease from spreading to other colonies.\n" +
                "4. Remove visible mummies and clean the hive interior.\n" +
                "5. Make the hive warmer and drier. Improve ventilation, tilt the hive slightly forward so water does not collect, place the colony where it gets sun, and avoid damp low areas or overgrown sites.\n" +
                "6. Reduce brood chilling and stress. Avoid long inspections during cold or wet weather, and do not overexpand the brood nest until the colony is strong enough to keep the brood warm.\n" +
                "7. Strengthen the colony with proper nutrition. Make sure the hive has enough food reserves.\n" +
                "8. Requeen if the outbreak is moderate, severe, or recurring. Replacing the queen with a hygienic or resistant stock is commonly recommended.\n" +
                "9. Reinspect after 7 to 14 days. Look for fewer new mummies, a drier brood nest, and a stronger adult bee population.\n" +
                "10. Keep the colony out of damp shade."
            "Dead Bees / Colony Loss" ->
                "1. Clean out the hive and debris\n2. Investigate for starvation or disease\n3. Sterilize equipment before reuse\n4. Seal hive to prevent robbing"
            "Not a bee" ->
                "1. Ensure good lighting and focus\n2. Crop image closer to the bees\n3. Clean camera lens\n4. Try capturing a different angle"
            else ->
                "1. Ensure good lighting and focus\n2. Crop image closer to the bees\n3. Clean camera lens\n4. Try capturing a different angle"
        }
    }
}
