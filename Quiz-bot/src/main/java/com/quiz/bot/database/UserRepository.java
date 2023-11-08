/*package com.quiz.bot.database;
//Next steep
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends CrudRepository<User, Long> {
    @Transactional
    @Modifying
    @Query("update tg_data t set t.msg_numb = t.msg_numb + 1 where t.id is not null and t.id = :id")
    void updateMsgNumberByUserId(@Param("id") long id);
}

 */