

loadSlotsExperimentData <- function(exp_prefix) {
    
    exp_files <- read_experiment(exp_prefix)
    
    column_spec <- cols_only(
        num_students = col_integer(),
        num_projects = col_integer(),
        num_slots = col_integer(),
        proj_pref_type = col_factor(),
        pregroup_type = col_factor(),
        mechanism = col_factor(),
        trial = col_factor(),
        duration_ms = col_integer(),
        profile_all = col_character(),
        project_pressure = col_factor()
    )
    
    map_dfr(exp_files, read_csv, col_types = column_spec) |>
        mutate(
            worst_rank = map(profile_all, worst_obtained_rank) |> unlist(),
            aupcr = pmap(list(num_projects, num_slots, profile_all), calc_aupcr) |> unlist()
        ) |>
        fix_pressure_levels()
}

loadSizeExperimentData <- function(exp_prefix) {

    exp_files <- read_experiment(exp_prefix)

    column_spec <- cols_only(
        num_students = col_integer(),
        num_projects = col_integer(),
        num_slots = col_integer(),
        proj_pref_type = col_factor(),
        pregroup_type = col_factor(),
        mechanism = col_factor(),
        trial = col_factor(),
        duration_ms = col_integer(),
        profile_all = col_character()
    )

    map_dfr(exp_files, read_csv, col_types = column_spec) |>
        mutate(
            worst_rank = map(profile_all, worst_obtained_rank) |> unlist(),
            aupcr = pmap(list(num_projects, num_slots, profile_all), calc_aupcr) |> unlist()
        ) |>
        augment_with_short_mechanism_name()
}

loadGroupSizeBoundsExperimentData <- function(exp_prefix) {

    exp_files <- read_experiment(exp_prefix)

    column_spec <- cols_only(
        num_students = col_integer(),
        num_projects = col_integer(),
        num_slots = col_factor(),
        proj_pref_type = col_factor(),
        pregroup_type = col_factor(),
        mechanism = col_factor(),
        trial = col_factor(),
        duration_ms = col_integer(),
        profile_all = col_character(),
        project_pressure = col_factor(),
        gsc_min = col_integer(),
        gsc_max = col_integer()
    )
    
    map_dfr(exp_files, read_csv, col_types = column_spec) |>
        mutate(
            worst_rank = map(profile_all, worst_obtained_rank) |> unlist(),
            aupcr = pmap(list(num_projects, num_slots, profile_all), calc_aupcr) |> unlist()
        ) |>
        augment_with_short_mechanism_name() |>
        fix_pressure_levels()
}

loadGroupingSoftExperimentData <- function(exp_prefix) {

    exp_files <- read_experiment(exp_prefix)

    column_spec <- cols_only(
        num_students = col_integer(),
        num_projects = col_integer(),
        num_slots = col_factor(),
        proj_pref_type = col_factor(),
        pregroup_type = col_factor(),
        mechanism = col_factor(),
        trial = col_factor(),
        duration_ms = col_integer(),
        profile_all = col_character(),
        profile_singles = col_character(),
        profile_pregrouped = col_character(),
        profile_unsatpregroup = col_character(),
        project_pressure = col_factor(),
        pregroup_proj_pref_type = col_factor()
    )

    read_single_exp <- function(filename) {
        read_csv(filename, col_types = column_spec) |>
            add_column(gd = determine_gd(filename))
    }

    d <- map_dfr(exp_files, read_single_exp) |>
        fix_pressure_levels()
    
    d$pregroup_proj_pref_type <- factor(d$pregroup_proj_pref_type, levels = c("IDENTICAL", "MIX", "DIFFERENT"))
    d$gd <- factor(d$gd, levels = c("MAX_ONLY", "MAX_TAPERED", "REALISTIC", "PAIR_TAPERED"))
    
    return(d)
}

loadHistoricalExperimentData <- function(exp_prefix) {
    
    exp_files <- read_experiment(exp_prefix)
    
    column_spec <- cols_only(
        id = col_factor(),
        
        num_students = col_integer(),
        num_projects = col_integer(),
        num_slots = col_integer(),
        
        mechanism = col_factor(),
        trial = col_integer(),
        
        duration_ms = col_integer(),
        
        profile_all = col_character(),
        profile_singles = col_character(),
        profile_pregrouped = col_character(),
        profile_unsatpregroup = col_character(),
        
        pregroup_proportion = col_double(),
        pregroup_sizes_distribution = col_character(),
        
        num_pregroups_fully_together = col_integer(),
        num_pregroups_max = col_integer()
    )
    
    d <- map_dfr(exp_files, read_csv, col_types = column_spec) |>
        augment_with_short_mechanism_name()
    
    return(d)
}


read_experiment <- function(exp_name) {
    
    root_dir_desktop = "D:\\Code\\Git repositories\\aidm-optimal-groups"
    root_dir_laptop = "C:\\Users\\Philipe\\Documents\\GitHub\\aidm-optimal-groups"
    
    root_dir = root_dir_desktop
    data_dir = paste(root_dir, "\\results\\thesis", sep="")
    
    exp_files <- list.files(data_dir, pattern = paste0(exp_name, "_.*\\.csv"), full.names = TRUE)
    return(exp_files)
}

determine_gd <- function(filename) {
    p <- ".*_gd\\[(.+)\\].csv$"
    sub(x = filename, pattern = p, replacement = "\\1")
}
