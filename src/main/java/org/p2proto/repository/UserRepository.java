package org.p2proto.repository;

import org.p2proto.entity.ExtendedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<ExtendedUser, String> {
}
