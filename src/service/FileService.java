package service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import common.tool.BeanUtils;
import common.tool.IdGenerator;
import entity.IFile;
import entity.ITag;
import jdbc.JDBCConnector;

public class FileService {
    private JDBCConnector conn;
    private IdGenerator id;

    public FileService() {
        conn = new JDBCConnector();
        id = new IdGenerator();
    }

    public void close() {
        conn.close();
    }

    /**
     * adding file to database
     *
     * @param file
     * @param belong folder
     */
    public void addFile(File file, String belong) {
        // String regex = "/\''/g"; remove all '
        String name = file.getName().replace('\'', ' ');
        String path = file.getPath().replace('\'', ' ');
        String sql = "INSERT or ignore into file(id,name,path,size,belong) values('" + id.next() + "','"
                + name + "','" + path + "','" + file.length() + "','" + belong + "');";
        conn.update(sql);
    }

    /**
     * rename the file
     * 
     * @param file
     */
    public void renameFile(IFile iFile) {
        //rename the real file
        File file = new File(iFile.getPath());
        File newFile = new File(file.getParent(), iFile.getName());
        file.renameTo(newFile);

        //rename the file in the database 
        String id = iFile.getId();
        String sql = "UPDATE file SET name = '" + newFile.getName() + "',path='"+newFile.getPath()+"' WHERE id = '" + id + "';";
        conn.update(sql);
        //reset
        iFile.setPath(newFile.getPath());
    }

    /**
     * delee the file
     *
     * @param file
     */
    public void removeFile(IFile file) {
        if (file.isDirectory()) {
            removeDir(file.getPath());
        } else {
            String sql = "DELETE FROM file WHERE id = '" + file.getId() + "';";
            conn.update(sql);
        }
    }

    /**
     * delete the folder
     * 
     * @param dir
     */
    public void removeDir(String dir) {
        String sql = "DELETE FROM file WHERE belong = '" + dir + "';";
        conn.update(sql);

    }

    /**
     * open the file
     *
     * @param path
     * @return true: exist, false: not exist
     */
    public boolean openDir(String path) {
        File file = new File(path);
        // 该路径不存在就返回
        if (!file.exists())
            return false;
        try {
            // 打开文件
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /*
     * check repeat file
     * 
     * @return <same size，files>
     */
    public Map<String, List<IFile>> getRepeatMap() {
        Map<String, List<IFile>> map = new HashMap<String, List<IFile>>();
        List<IFile> files = getAllFiles();
        if (!files.isEmpty()) {
            map = files.stream().collect(Collectors.groupingBy(IFile::getSize));
            map = map.entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()));
        }
        return map;
    }

    /**
     * get all files with tags
     */
    public List<IFile> getAllFiles(){
        String sql = "SELECT * FROM file;";
        return getFilesWithTags(sql);
    }

    /**
     * fuzzy search
     */
    public List<IFile> search(String text) {
        String sql = "SELECT * FROM file WHERE name LIKE '%" + text + "%';";
        return getFilesWithTags(sql);
    }   

    /**
     * get all files with tags
     */
    private List<IFile> getFilesWithTags(String sql){
        ResultSet rs = conn.select(sql);
        Map<String, IFile> map = new HashMap<>();
        try {
            while (rs.next()) {
                IFile file = BeanUtils.getFile(rs);
                map.put(file.getId(), file);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        if (!map.isEmpty()) {
            String sqlFormat = "SELECT ft.file_id, ft.tag_id as id, t.name, t.'group' FROM file_tag ft LEFT JOIN tag t ON ft.tag_id = t.id WHERE file_id in (%s);";
            String sql2 = String.format(sqlFormat, map.keySet().stream().collect(Collectors.joining(",")));
            ResultSet rs2 = conn.select(sql2);
            try {
                while (rs2.next()) {
                    String file_id = rs2.getString("file_id");
                    ITag tag = BeanUtils.getTag(rs2);
                    map.get(file_id).add(tag);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                conn.close();
            }
            return map.values().stream().collect(Collectors.toList());
        } else {
            return null;
        }
    }

    /**
     * 
     * @return untagged files
     */
    public List<IFile> getUntaggedFiles() {
        String sql = "SELECT * FROM file WHERE id not in (SELECT DISTINCT file_id FROM file_tag);";
        List<IFile> files = new ArrayList<IFile>();
        ResultSet rs = conn.select(sql);
        try {
            while (rs.next()) {
                IFile file = BeanUtils.getFile(rs);
                files.add(file);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return files;
    }

    public void updateFiles(List<IFile> files) {
        StringBuffer buffer = new StringBuffer();
        files.forEach(file->{
            String sql = "UPDATE file SET path='"+file.getPath()+"', belong = '"+file.getBelong()+"' WHERE id = '" + file.getId() + "';";
            buffer.append(sql);
        });
        conn.update(buffer.toString());
    }
}
