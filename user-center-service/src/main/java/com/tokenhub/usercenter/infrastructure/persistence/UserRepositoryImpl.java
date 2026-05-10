package com.tokenhub.usercenter.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.usercenter.domain.user.UserAccount;
import com.tokenhub.usercenter.domain.user.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

  private final UserMapper mapper;

  public UserRepositoryImpl(UserMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public UserAccount save(UserAccount user) {
    UserPo po = new UserPo();
    po.setEmail(user.getEmail());
    po.setPasswordHash(user.getPasswordHash());
    po.setDisplayName(user.getDisplayName());
    po.setStatus(user.getStatus());
    mapper.insert(po);
    return user.withId(po.getId());
  }

  @Override
  public Optional<UserAccount> findByEmail(String email) {
    UserPo po = mapper.selectOne(new LambdaQueryWrapper<UserPo>().eq(UserPo::getEmail, email));
    return Optional.ofNullable(po).map(this::toDomain);
  }

  @Override
  public Optional<UserAccount> findById(long id) {
    return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
  }

  @Override
  public void updatePasswordHash(long userId, String newPasswordHash) {
    UserPo patch = new UserPo();
    patch.setId(userId);
    patch.setPasswordHash(newPasswordHash);
    mapper.updateById(patch);
  }

  private UserAccount toDomain(UserPo po) {
    return new UserAccount(po.getId(), po.getEmail(), po.getPasswordHash(), po.getDisplayName(), po.getStatus());
  }
}
