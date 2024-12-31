package org.p2proto.repository;

import org.p2proto.entity.ExtendedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<ExtendedUser, UUID> {
}
