package world.xuewei.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import world.xuewei.component.OssClient;
import world.xuewei.dto.RespResult;
import world.xuewei.entity.User;
import world.xuewei.utils.Assert;

import java.io.IOException;
import java.io.IOException;

/**
 * 文件控制器
 *
 * @author XUEW
 */
@RestController
@RequestMapping("/file")
/*
 * public class FileController extends BaseController<User> {
 * 
 * @Autowired private OssClient ossClient;
 * 
 *//**
	 * 上传文件
	 *//*
		 * @PostMapping("/upload") public RespResult upload(@RequestParam("file")
		 * MultipartFile file) throws IOException { String url = ossClient.upload(file,
		 * String.valueOf(loginUser.getId())); if (Assert.isEmpty(url)) { return
		 * RespResult.fail("上传失败", url); } return RespResult.success("上传成功", url); } }
		 */

public class FileController extends BaseController {

	@Autowired
	private OssClient ossClient;

	@PostMapping("/upload")
	public RespResult upload(@RequestParam("file") MultipartFile file) throws IOException {
		// 通过传递 true 作为 useLocal 参数来使用本地存储
		String url = ossClient.upload(file, String.valueOf(loginUser.getId()));
		if (url == null) {
			return RespResult.fail("上传失败");
		}
		return RespResult.success("上传成功", url);
	}
}
