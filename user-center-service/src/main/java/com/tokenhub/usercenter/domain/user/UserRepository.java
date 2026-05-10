package com.tokenhub.usercenter.domain.user;

import java.util.Optional;

public interface UserRepository {

  UserAccount save(UserAccount user);

  Optional<UserAccount> findByEmail(String email);

  Optional<UserAccount> findById(long id);

  void updatePasswordHash(long userId, String newPasswordHash);
}
