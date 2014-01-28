package fr.xebia.kouignamann.pi.draft.data

import com.sleepycat.persist.model.Entity
import com.sleepycat.persist.model.PrimaryKey

@Entity
class Voter {
    @PrimaryKey
    private String nfcId
    private String name
}
