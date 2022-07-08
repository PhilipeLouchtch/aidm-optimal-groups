mechanism_name_map = new.env()
mechanism_name_map[["Chiarandini w Fair pregrouping owa - anyClique_softGrp"]] = "Fair"
mechanism_name_map[["Chiarandini w Fair pregrouping IMPR owa - anyClique_softGrp"]] = "Fair"
mechanism_name_map[["Chiarandini w Fair pregrouping IMPR owa - anyClique_softGrpEps"]] = "Fair"
mechanism_name_map[["Fair (impr-eps) - owa - anyClique_softGrpEps"]] = "Fair"
mechanism_name_map[["BepSys (reworked) - Borda"]] = "BEPSys"
mechanism_name_map[["Chiaranini MiniMax-OWA - no_grouping"]] = "Chiarandini"
mechanism_name_map[["Chiaranini MiniMax-OWA - anyClique_hardGrp"]] = "Chiarandini"
mechanism_name_map[["Chiaranini MiniMax-OWA - anyClique_softGrp"]] = "Chiarandini"
mechanism_name_map[["SDPC-S (project slots)"]] = "SDPC-S"

mechanism_name_map_short = new.env()
mechanism_name_map_short[["Chiarandini w Fair pregrouping owa - anyClique_softGrp"]] = "Fair (soft)"
mechanism_name_map_short[["Chiarandini w Fair pregrouping IMPR owa - anyClique_softGrp"]] = "Fair (impr)(soft)"
mechanism_name_map_short[["Chiarandini w Fair pregrouping IMPR owa - anyClique_softGrpEps"]] = "Fair"
mechanism_name_map_short[["Fair (impr-eps) - owa - anyClique_softGrpEps"]] = "Fair"
mechanism_name_map_short[["BepSys (reworked) - Borda"]] = "BEPSys"
mechanism_name_map_short[["Chiaranini MiniMax-OWA - no_grouping"]] = "Chiarandini"
mechanism_name_map_short[["Chiaranini MiniMax-OWA - anyClique_softGrp"]] = "Chiarandini"	
mechanism_name_map_short[["Chiaranini MiniMax-OWA - anyClique_hardGrp"]] = "Chiarandini"
mechanism_name_map_short[["SDPC-S (project slots)"]] = "SDPC-S"

nice_proj_pref_name_map = new.env()
nice_proj_pref_name_map[["singleton"]] = "unanimous"
nice_proj_pref_name_map[["linear_perturbed_1"]] = "nearly unanimous"
nice_proj_pref_name_map[["linear_perturbed_4"]] = "mostly unanimous"
nice_proj_pref_name_map[["random"]] = "random"
nice_proj_pref_name_map[["realistic"]] = "3-peaked"

augment_with_short_mechanism_name <- function(data) {
    
    d <- data |> mutate(
        mechanism_short = map(mechanism, ~ mechanism_name_map[[ levels(mechanism)[.] ]] ) |>
            unlist() |>
            factor(levels = c("BEPSys", "SDPC-S", "Chiarandini", "Fair"))
    )
    
    d
}

augment_with_nice_projectpref_name <- function(data) {
    
    d <- data |> mutate(
        projectpref_short = map(proj_pref_type, ~ nice_proj_pref_name_map[[ levels(proj_pref_type)[.] ]]) |>
            unlist() |>
            factor(levels = c("unanimous", "nearly unanimous", "mostly unanimous", "3-peaked", "random"))
    )
    
    d
}

fix_pressure_levels <- function(data) {
    data$project_pressure <- factor(data$project_pressure, levels = c("TIGHT", "MID", "LOOSE"))
    data
}
