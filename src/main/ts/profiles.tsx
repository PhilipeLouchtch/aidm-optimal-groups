type Profile = number[]
type ProfileDiff = Array<`+${number}` | `-${number}` | " 0">

function parse(profile: string): Profile {
  const matches = profile.match(/\b\d+\b/g) ?? []

  if (matches.length === 0 && profile !== "") {
    console.warn(`input '${profile}' parsed as []`)
  }

  return matches.map((i) => Number.parseInt(i))
}

function atOrDefault<T>(arr: Array<T>, i: number, def: T): T {
  return arr.length > i ? arr[i] : def
}

function diff(left: Profile, right: Profile): ProfileDiff {
  const d = new Array<ProfileDiff[number]>(Math.max(left.length, right.length))
  d.fill(" 0")

  for (let i = 0; i < d.length; i++) {
    const v = (left[i] ?? 0) - (right[i] ?? 0)
    d[i] = v == 0 ? " 0" : v > 0 ? `+${v}` : `-${-1 * v}`
  }

  return d
}

function add(left: Profile, right: Profile): Profile {
  const r = new Array<number>(Math.max(left.length, right.length))
  r.fill(0)

  for (let i = 0; i < r.length; i++) {
    r[i] = (left[i] ?? 0) + (right[i] ?? 0)
  }

  return r
}

function sum(prof: Profile): number {
  return prof.reduce((prev, curr) => prev + curr)
}

function isDiff(p: any[]): p is ProfileDiff {
  if (typeof p[0] === "string") {
    return true
  }

  return false
}

function print(p: string | ProfileDiff | Profile, prefix: string = "") {
  if (typeof p === "string") {
    console.log(p)
  } else if (isDiff(p)) {
    console.log(prefix + diffToString(p))
  } else {
    console.log(prefix + profToString(p))
  }
}

function diffToString(diff: ProfileDiff) {
  return JSON.stringify(diff).replaceAll('"', "").replaceAll(",", " ")
}

function profToString(prof: Profile) {
  return JSON.stringify(prof).replaceAll(",", " ")
}

// print(parse(" [86 59 4 0 0 0 1 0 4] "))
// throw new Error("done testing")
// const variant = parse("22 27 13 4 5 7 10 0 3 0 0 0 0 0 5 16")

// const diffd = diff(max, variant)

// print(
//   diff(
//     parse("22 27 13 4 5 7 10 0 3 0 0 0 0 0 5 16"), // var
//     parse("18 20 22 4 8 3 12 0 3 0 0 0 0 0 5 17 0"), // max
//   ),
// )

// print(
//   add(
//     parse("17 8 5 0 0 5 5 0 3 0 0 0 0 0 4 5 5"),
//     parse("12 17 11 0 4 2 0 0 0 0 0 0 0 0 1 8"),
//   ),
// )

type Scenario = "any" | "except" | "max"
type StudClass = "solo" | "preg"
type Mechanism = "fair" | "chia"

const instances_raw = {
  CE10: {
    solo: {
      fair: {
        max: parse("9 8 4 6 4 3 0 1 0 1 0 0 0 0 0 1"),
        except: parse("12 9 5 2 1 3 0 3 0 0 0 0 0 0 0 2"),
        any: parse("9 10 5 4 2 2 0 2 0 0 0 0 0 0 1 2"),
      },

      chia: {
        max: parse("8 11 5 3 3 3 0 2 0 0 0 0 0 0 0 2"),
        except: parse("13 11 2 3 1 4 0 2 0 0 0 0 0 0 0 1"),
        any: parse("7 6 5 8 2 3 0 3 0 0 0 0 0 0 1 2"),
      },
    },

    preg: {
      fair: {
        max: {
          sat: parse("17 8 5 0 0 5 5 0 3 0 0 0 0 0 4 5 5"),
          unsat: parse("12 17 11 0 4 2 0 0 0 0 0 0 0 0 1 8"),
        },
        except: {
          sat: parse("13 13 6 4 5 5 5 0 3 0 0 0 0 0 4 5 0 1 2"),
          unsat: parse("12 6 13 0 2 2 1 0 0 0 0 0 0 0 1 9"),
        },
        any: {
          sat: parse("5 4 2 0 0 0 5 0 3 0 0 0 0 0 4 5"),
          unsat: parse("12 22 17 6 7 3 5 0 0 0 0 0 0 0 1 11"),
        },
      },

      chia: {
        max: {
          sat: parse("5 12 6 4 0 0 10 0 3 0 0 0 0 0 4 9"),
          unsat: parse("13 8 16 0 8 3 2 0 0 0 0 0 0 0 1 8"),
        },
        except: {
          sat: parse("10 12 8 4 3 0 13 0 3 0 0 0 0 0 4 9"),
          unsat: parse("8 5 14 0 5 2 2 0 0 0 0 0 0 0 1 9"),
        },
        any: {
          sat: parse("8 4 0 0 0 0 5 0 3 0 0 0 0 0 4 5"),
          unsat: parse("14 23 13 4 5 7 5 0 0 0 0 0 0 0 1 11"),
        },
      },
    },
  },

  CE11: {
    solo: {
      fair: {
        max: parse(" [86 59 4 0 0 0 1 0 4] "),
        except: parse(" [84 59 5 0 0 0 1 0 5] "),
        any: parse(" [88 53 11 0 0 0 1 0 1] "),
      },

      chia: {
        max: parse(" [88 50 5 0 2 1 1 0 7] "),
        except: parse("[80 50 8 0 1 3 3 4 5] "),
        any: parse(" [83 56 7 0 0 0 1 0 7] "),
      },
    },

    preg: {
      fair: {
        max: {
          sat: parse(" [6 6 6 0 0 0 0 0 6] "),
          unsat: parse(" [34 31 8 0 0 0 0 0 1]"),
        },
        except: {
          sat: parse("[28 18 6 0 0 0 0 0 6] "),
          unsat: parse("[13 20 7] "),
        },
        any: {
          sat: parse(" [42 37 9 0 0 0 0 0 10]"),
          unsat: parse("[]"),
        },
      },

      chia: {
        max: {
          sat: parse("[3 12 6 1 0 0 4] "),
          unsat: parse(" [32 27 8 0 1 0 0 0 4] "),
        },
        except: {
          sat: parse("[27 17 7 1 0 0 0 0 6] "),
          unsat: parse(" [13 15 7 0 1 2 1 1]"),
        },
        any: {
          sat: parse("[40 39 15 0 0 0 0 0 4] "),
          unsat: parse("[]"),
        },
      },
    },
  },

  CE18: {
    solo: {
      fair: {
        max: parse(" [26 14 10] "),
        except: parse(" [18 20 12] "),
        any: parse("[19 20 11]"),
      },

      chia: {
        max: parse(" [19 19 12] "),
        except: parse(" [18 19 13] "),
        any: parse(" [16 20 14]"),
      },
    },

    preg: {
      fair: {
        max: {
          sat: parse(" [8 21 10 0 4 0 1] "),
          unsat: parse(" [8 4 5] "),
        },
        except: {
          sat: parse("[16 27 8 3 0 3] "),
          unsat: parse(" [3 1] "),
        },
        any: {
          sat: parse(" [12 30 12 7] "),
          unsat: parse("[]"),
        },
      },

      chia: {
        max: {
          sat: parse(" [8 33 9] "),
          unsat: parse(" [3 4 4] "),
        },
        except: {
          sat: parse(" [11 33 9] "),
          unsat: parse(" [1 3 4] "),
        },
        any: {
          sat: parse(" [13 29 19] "),
          unsat: parse("[]"),
        },
      },
    },
  },
}

const data_raw = instances_raw["CE11"]

type DataType = {
  [key in StudClass]: {
    [key in Mechanism]: {
      [key in Scenario]: Profile
    }
  }
}

const data: DataType = {
  solo: {
    fair: {
      max: data_raw.solo.fair.max,
      except: data_raw.solo.fair.except,
      any: data_raw.solo.fair.any,
    },
    chia: {
      max: data_raw.solo.chia.max,
      except: data_raw.solo.chia.except,
      any: data_raw.solo.chia.any,
    },
  },
  preg: {
    fair: {
      max: add(data_raw.preg.fair.max.sat, data_raw.preg.fair.max.unsat),
      except: add(
        data_raw.preg.fair.except.sat,
        data_raw.preg.fair.except.unsat,
      ),
      any: add(data_raw.preg.fair.any.sat, data_raw.preg.fair.any.unsat),
    },
    chia: {
      max: add(data_raw.preg.chia.max.sat, data_raw.preg.chia.max.unsat),
      except: add(
        data_raw.preg.chia.except.sat,
        data_raw.preg.chia.except.unsat,
      ),
      any: add(data_raw.preg.chia.any.sat, data_raw.preg.chia.any.unsat),
    },
  },
}

// print(data_raw.)

const classes: StudClass[] = ["solo", "preg"]
const mechanisms: Mechanism[] = ["fair", "chia"]
const scenarios: Scenario[] = ["except", "any"]

for (const studClass of classes) {
  print(`---${studClass}---`)
  for (const scenario of scenarios) {
    print(`-> ${scenario.toUpperCase()}`)
    for (const mech of mechanisms) {
      print(
        diff(data[studClass][mech][scenario], data[studClass][mech]["max"]),
        `${mech}: `,
      )
    }
  }
}
