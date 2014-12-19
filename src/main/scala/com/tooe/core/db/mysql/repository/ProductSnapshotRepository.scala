package com.tooe.core.db.mysql.repository

import org.springframework.data.jpa.repository.JpaRepository
import com.tooe.core.db.mysql.domain.ProductSnapshot
import java.math.BigInteger

trait ProductSnapshotRepository extends JpaRepository[ProductSnapshot, BigInteger]{
}
