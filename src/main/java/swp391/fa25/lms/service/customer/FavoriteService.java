package swp391.fa25.lms.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Favorite;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FavoriteRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private AccountRepository accountRepository;

    /**
     * Toggle favorite: nếu đã yêu thích -> xóa, nếu chưa -> thêm
     * @return true nếu thêm, false nếu xóa
     */
    public boolean toggleFavorite(Account account, Tool tool) {
        // Tìm xem đã favorite chưa
        return favoriteRepository.findByAccountAndTool(account, tool)
                .map(fav -> {
                    favoriteRepository.delete(fav);  // nếu tồn tại -> xóa
                    return false;
                })
                .orElseGet(() -> {
                    Favorite newFav = new Favorite();
                    newFav.setAccount(account);
                    newFav.setTool(tool);
                    favoriteRepository.save(newFav); // nếu chưa tồn tại -> thêm
                    return true;
                });
    }

    /**
     * Lấy danh sách tool đã yêu thích của account
     */
    public List<Tool> getFavoriteTools(Account account) {
        return favoriteRepository.findByAccount(account).stream()
                .map(Favorite::getTool)
                .toList();
    }

    /**
     * Đếm số favorite của account
     */
    public long countFavoritesByAccount(Account account) {
        return favoriteRepository.countByAccount(account);
    }

    /**
     * Count trực tiếp by ID
     */
    public long countByAccountId(Long accountId) {
        if (accountId == null) return 0L;
        return favoriteRepository.countByAccountId(accountId);
    }

    /**
     * Lấy Set tool IDs yêu thích của account
     */
    public Set<Long> getFavoriteToolIds(Account account) {
        return favoriteRepository.findByAccount(account).stream()
                .map(fav -> fav.getTool().getToolId())
                .collect(Collectors.toSet());
    }

    /**
     * Fix: Lấy list Tool favorite của account (dùng ID để tránh dummy Account)
     */
    public List<Tool> getFavoritesByAccount(Long accountId) {
        // Thêm method findByAccountId ở Repo (xem dưới)
        return favoriteRepository.findByAccountId(accountId).stream()
                .map(Favorite::getTool)
                .toList();
    }

}
