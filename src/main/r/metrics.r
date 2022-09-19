profile_histo <- function(cell) {
    if (is.na(cell))
        cell <- ""
    
    as.double(unlist(scan(text = cell, what = "double", sep = "|", quiet = TRUE)))
}

rank_profile <- function(cell) {
    profile_histo(cell) %>%
        imap(~ list(rep.int(.y, .x))) %>%
        unlist
}

worst_obtained_rank <- function(xa) {
    profile_histo(xa) %>%
        length %>%
        as.integer
}

calc_aupcr <- function(num_projs, num_slots, raw_profile) {
    # if (is.na(raw_profile))
    #     return (NA)
    
    worst_rank <- worst_obtained_rank(raw_profile)
    
    profile <- profile_histo(raw_profile)
    num_studs <- sum(profile)
    cumsum_upto_worst = cumsum(profile)
    
    # The cumsum of a profile is not complete, because out profiles are not complete
    # Let "3|2|1" be a profile of 6 students in a setting with 10 projects,
    # then cumsum will only give us the following: [3, 5, 6] but we need [3, 5, 6, 6, 6, 6, 6, 6, 6, 6]
    # because we set the "R" (ranks) to be the #projects in order to easily compare and compute AUPCR's between
    # generated dataset instances with same paramers
    AUPC <- sum(cumsum_upto_worst) + (num_projs - worst_rank) * cumsum_upto_worst[worst_rank]
    aupcr <- AUPC / (num_projs * num_studs)
}

avg_rank <- function(raw_profile) {
    profile <- profile_histo(raw_profile)
    num_studs <- sum(profile)
    
    profile %>%
        imap(~ .x * .y / num_studs) %>%
        unlist %>%
        sum
}

avg_weighted_rank <- function(raw_profile) {
    profile <- profile_histo(raw_profile)
    num_studs <- sum(profile)
    
    profile %>%
        imap(~ .x * .y^2 / num_studs) %>%
        unlist %>%
        sum
}
