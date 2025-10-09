package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.ToolRepository;

import java.util.List;

@Service
public class ToolService {
    @Autowired
    private ToolRepository toolRepository;

    public List<Tool> findAll() {
        return toolRepository.findAll();
    }

    public Tool save(Tool tool) {
        return toolRepository.save(tool);
    }

    public List<Tool> filterTools(String toolName,
                                  Long categoryId,
                                  String primarySort, String primaryOrder,
                                  String secondarySort, String secondaryOrder) {

        Sort sort;

        Sort primary = buildSort(primarySort, primaryOrder);
        Sort secondary = buildSort(secondarySort, secondaryOrder);


        if (!primary.isUnsorted() && !secondary.isUnsorted()) sort = primary.and(secondary);
        else if (!primary.isUnsorted()) sort = primary;
        else sort = secondary;

        if (toolName == null || toolName.isBlank()) {
            return (categoryId != null)
                    ? toolRepository.findAllByCategory_CategoryId(categoryId, sort)
                    : toolRepository.findAll(sort);
        }

        if (categoryId == null) {
            return toolRepository.findAllByToolNameContainingIgnoreCase(toolName, sort);
        }
        return toolRepository.findAllByToolNameContainingIgnoreCaseAndCategory_CategoryId(toolName, categoryId, sort);
    }

    private Sort buildSort(String sortBy, String order) {

        if (sortBy == null || order == null) return Sort.unsorted();

        return switch (sortBy.toLowerCase()) {
            case "priceorder" -> {
                if ("asc".equalsIgnoreCase(order)) {
                    yield Sort.by("price").ascending();
                } else if ("desc".equalsIgnoreCase(order)) {
                    yield Sort.by("price").descending();
                } else {
                    yield Sort.unsorted();
                }
            }

            case "updatedat", "updateat", "updatedate" -> {
                if ("asc".equalsIgnoreCase(order)) {
                    yield Sort.by("updatedAt").ascending();
                } else if ("desc".equalsIgnoreCase(order)) {
                    yield Sort.by("updatedAt").descending();
                } else {
                    yield Sort.unsorted();
                }
            }

            default -> Sort.unsorted();
        };

    }

    public Tool findById(long id) {
        return toolRepository.findByToolId(id);
    }
    public List<Tool> availableTools(Tool.Status status) {
        return toolRepository.findByStatus(status);
    }
    public List<Tool> filterToolsForModerator(String toolName, Long categoryId, String status) {
        Tool.Status toolStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                toolStatus = Tool.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                ignored.printStackTrace();
            }
        }

        if (toolName == null || toolName.isBlank()) toolName = null;
        if (categoryId != null && categoryId <= 0) categoryId = null;

        return toolRepository.filterToolsForModerator(toolName, categoryId, toolStatus);
    }
}

