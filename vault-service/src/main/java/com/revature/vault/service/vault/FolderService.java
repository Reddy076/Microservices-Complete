package com.revature.vault.service.vault;

import com.revature.vault.dto.FolderDTO;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.model.vault.Folder;
import com.revature.vault.repository.FolderRepository;
import com.revature.vault.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

  private final FolderRepository folderRepository;
  private final UserClient userClient;

  @Transactional(readOnly = true)
  public List<FolderDTO> getFolders(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    List<Folder> folders = folderRepository.findByUserIdAndParentFolderIsNull(user.getId());
    return folders.stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public FolderDTO getFolderById(Long id, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    Folder folder = folderRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));
    return mapToDTO(folder);
  }

  @Transactional
  public FolderDTO createFolder(String name, Long parentFolderId, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    Folder parentFolder = null;
    if (parentFolderId != null) {
      parentFolder = folderRepository.findByIdAndUserId(parentFolderId, user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Parent Folder", "id", parentFolderId));
    }

    Folder folder = Folder.builder()
        .name(name)
        .userId(user.getId())
        .parentFolder(parentFolder)
        .build();

    Folder savedFolder = folderRepository.save(folder);
    return mapToDTO(savedFolder);
  }

  @Transactional
  public FolderDTO updateFolder(Long id, String name, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    Folder folder = folderRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

    folder.setName(name);
    Folder savedFolder = folderRepository.save(folder);
    return mapToDTO(savedFolder);
  }

  @Transactional
  public FolderDTO moveFolder(Long id, Long newParentId, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    Folder folder = folderRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

    Folder newParent = null;
    if (newParentId != null) {

      if (folder.getId().equals(newParentId)) {
        throw new IllegalArgumentException("Cannot move a folder into itself");
      }

      newParent = folderRepository.findByIdAndUserId(newParentId, user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Parent Folder", "id", newParentId));
    }

    folder.setParentFolder(newParent);
    Folder savedFolder = folderRepository.save(folder);
    return mapToDTO(savedFolder);
  }

  @Transactional
  public void deleteFolder(Long id, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    Folder folder = folderRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

    folderRepository.delete(folder);
  }

  private FolderDTO mapToDTO(Folder folder) {
    return FolderDTO.builder()
        .id(folder.getId())
        .name(folder.getName())
        .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
        .subfolders(folder.getSubfolders().stream().map(this::mapToDTO).collect(Collectors.toList()))
        .createdAt(folder.getCreatedAt())
        .updatedAt(folder.getUpdatedAt())
        .build();
  }

}



