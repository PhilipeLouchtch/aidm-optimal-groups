
test <- function() {
    
    raw_profile <- "166|34"
    num_projs <- 20
    num_studs <- 200
    num_slots <- 1
    
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

test() |> print()
