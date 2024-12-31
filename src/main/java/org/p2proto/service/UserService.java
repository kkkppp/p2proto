package org.p2proto.service;

import org.p2proto.entity.ExtendedUser;
import org.p2proto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public ExtendedUser getUserById(UUID id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<ExtendedUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Autowired
    public UserService() {
    }

    public ExtendedUser saveUser(ExtendedUser user) {
      return userRepository.save(user);
    }

    public ExtendedUser updateUser(ExtendedUser user) {
      return userRepository.save(user);
    }

    public void deleteUser(UUID id) {
      userRepository.deleteById(id);
    }
}
