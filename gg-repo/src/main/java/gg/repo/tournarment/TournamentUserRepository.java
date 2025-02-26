package gg.repo.tournarment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import gg.data.tournament.Tournament;
import gg.data.tournament.TournamentUser;
import gg.data.user.User;

public interface TournamentUserRepository extends JpaRepository<TournamentUser, Long> {

	int countByTournamentAndIsJoined(Tournament tournament, boolean isJoined);

	List<TournamentUser> findAllByTournament(Tournament tournament);

	List<TournamentUser> findAllByTournamentAndIsJoined(Tournament tournament, boolean isJoined);

	List<TournamentUser> findAllByTournamentId(Long tournamentId);

	Optional<TournamentUser> findByTournamentIdAndUserId(Long tournamentId, Long userId);

	List<TournamentUser> findAllByUser(User user);
}
