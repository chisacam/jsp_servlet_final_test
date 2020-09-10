package board;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;

@WebServlet("/board/*")
public class BoardController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static String ARTICLE_IMAGE_REPO = "D:\\web_programming\\board\\article_image"; // 게시글 이미지 저장주소
	BoardService boardService;
	ArticleVO articleVO;

	public void init(ServletConfig config) throws ServletException {
		boardService = new BoardService();
		articleVO = new ArticleVO();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		doHandle(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		doHandle(request, response);
	}

	private void doHandle(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		String nextPage = "";
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		HttpSession session;
		String action = request.getPathInfo(); // 프로젝트 context root 이후 접속주소
		System.out.println("action:" + action);
		try {
			List<ArticleVO> articlesList = new ArrayList<ArticleVO>();
			if (action==null){// borad root로 접속했을때, 그리고 프로젝트 root로 접속 했을때도 이쪽으로 포워딩함(index.jsp)
				String _section=request.getParameter("section");
				String _pageNum=request.getParameter("pageNum");
				int section = Integer.parseInt(((_section==null)? "1":_section) ); 
				int pageNum = Integer.parseInt(((_pageNum==null)? "1":_pageNum)); // section, pageNum이 각각 없으면 기본값 1
				Map<String, Integer> pagingMap = new HashMap<String, Integer>();
				pagingMap.put("section", section);
				pagingMap.put("pageNum", pageNum);
				Map articlesMap=boardService.listArticles(pagingMap);
				articlesMap.put("section", section);
				articlesMap.put("pageNum", pageNum);
				request.setAttribute("articlesMap", articlesMap);
				nextPage = "/boardjsp/listArticles.jsp";
				}else if(action.equals("/listArticles.do")){ // root접속과 동일
				String _section=request.getParameter("section");
				String _pageNum=request.getParameter("pageNum");
				int section = Integer.parseInt(((_section==null)? "1":_section) );
				int pageNum = Integer.parseInt(((_pageNum==null)? "1":_pageNum));
				Map pagingMap=new HashMap();
				pagingMap.put("section", section);
				pagingMap.put("pageNum", pageNum);
				Map articlesMap=boardService.listArticles(pagingMap);
				articlesMap.put("section", section);
				articlesMap.put("pageNum", pageNum);
				request.setAttribute("articlesMap", articlesMap);
				nextPage = "/boardjsp/listArticles.jsp";
			} else if (action.equals("/articleForm.do")) {
				nextPage = "/boardjsp/articleForm.jsp";
			} else if (action.equals("/addArticle.do")) {
				int articleNO = 0;
				PrintWriter pw = response.getWriter();
				session = request.getSession();
				String id = (String) session.getAttribute("id"); // 작성자 확인을 위해 세션 확인, 로그인된 사용자만 글쓰기가 가능함
				if(id == null) {
					pw.print("<script>" + "  alert('로그인 후에 글을 쓸 수 있습니다.');" + " location.href='" + request.getContextPath()
					+ "/board/listArticles.do';" + "</script>");
					return;
				}
				Map<String, String> articleMap = upload(request, response); // 업로드 받은 이미지 디스크에 저장
				String title = articleMap.get("title");
				String content = articleMap.get("content");
				String imageFileName = articleMap.get("imageFileName"); // 게시글 정보들 받기
				articleVO.setParentno(0);
				articleVO.setId(id);
				articleVO.setTitle(title);
				articleVO.setContent(content);
				articleVO.setImagefilename(imageFileName);
				articleNO = boardService.addArticle(articleVO); // 받아온 데이터를 db에 저장
				if (imageFileName != null && imageFileName.length() != 0) {
					File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
					File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
					destDir.mkdirs();
					FileUtils.moveFileToDirectory(srcFile, destDir, true);
				}

				pw.print("<script>" + "  alert('글 작성을 성공했습니다.');" + " location.href='" + request.getContextPath()
						+ "/board/listArticles.do';" + "</script>");

				return;
			} else if (action.equals("/viewArticle.do")) {
				String articleNO = request.getParameter("articleNO"); // 봐야하는 게시글 id를 받아옴
				articleVO = boardService.viewArticle(Integer.parseInt(articleNO)); // 게시글 id를 db에 조회해서 정보를 받아옴
				request.setAttribute("article", articleVO); // 받아온 정보를 응답정보에 넣음
				nextPage = "/boardjsp/viewArticle.jsp";
			} else if (action.equals("/modArticle.do")) {
				Map<String, String> articleMap = upload(request, response);
				int articleNO = Integer.parseInt(articleMap.get("articleNO")); // 수정할 페이지 번호를 받아옴
				articleVO.setArticleno(articleNO);
				String title = articleMap.get("title");
				String content = articleMap.get("content");
				String imageFileName = articleMap.get("imageFileName");
				String writer = boardService.getWriter(articleNO); // 게시글 작성자를 db에서 조회해 받아옴
				
				session = request.getSession();
				String id = (String) session.getAttribute("id"); // 현재 수정을 요청한 유저의 id를 받아옴
				PrintWriter pw = response.getWriter();
				if(writer.equals(id)) { // 요청한 유저와 작성한 유저의 id를 비교함. 다른 유저거나 비로그인 유저면 수정불가
					articleVO.setParentno(0);
					articleVO.setId(id);
					articleVO.setTitle(title);
					articleVO.setContent(content);
					articleVO.setImagefilename(imageFileName);
					boardService.modArticle(articleVO); // 기존 글의 내용을 변경한다는것 외에는 새글작성과 거의 유사
					if (imageFileName != null && imageFileName.length() != 0) {
						String originalFileName = articleMap.get("originalFileName");
						File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
						File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
						destDir.mkdirs();
						FileUtils.moveFileToDirectory(srcFile, destDir, true);
						;
						File oldFile = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO + "\\" + originalFileName);
						oldFile.delete();
					}

					pw.print("<script>" + "  alert('글 수정을 성공했습니다.');" + " location.href='" + request.getContextPath()
							+ "/board/viewArticle.do?articleNO=" + articleNO + "';" + "</script>");
				} else {
					pw.print("<script>" + "  alert('작성자 본인만 수정할 수 있습니다.');" + " location.href='" + request.getContextPath()
					+ "/board/listArticles.do';" + "</script>");
				}
				return;
			} else if (action.equals("/removeArticle.do")) {
				session = request.getSession();
				int articleNO = Integer.parseInt(request.getParameter("articleNO"));
				String writer = boardService.getWriter(articleNO);
				String id = (String) session.getAttribute("id");
				PrintWriter pw = response.getWriter();
				if(writer.equals(id)) { // 글작성자 본인의 id와 로그인 유저의 id가 일치하는지 검사, 맞으면 삭제
					List<Integer> articleNOList = boardService.removeArticle(articleNO);
					for (int _articleNO : articleNOList) {
						File imgDir = new File(ARTICLE_IMAGE_REPO + "\\" + _articleNO);
						if (imgDir.exists()) {
							FileUtils.deleteDirectory(imgDir); // 해당 게시글의 이미지가 저장된 폴더 삭제
						}
					}
				

				pw.print("<script>" + "  alert('글 삭제를 완료했습니다.');" + " location.href='" + request.getContextPath()
						+ "/board/listArticles.do';" + "</script>");
				} else {
					pw.print("<script>" + "  alert('작성자 본인만 삭제할 수 있습니다.');"+ " location.href='" + request.getContextPath()
					+ "/board/listArticles.do';" + "</script>");
				}
				return;

			} else if (action.equals("/replyForm.do")) {
				int parentNO = Integer.parseInt(request.getParameter("parentNO"));
				PrintWriter pw = response.getWriter();
				session = request.getSession();
				session.setAttribute("parentNO", parentNO);
				String id = (String)session.getAttribute("id");
				if(id == null) {
					pw.print("<script>" + "  alert('로그인 후에 답글을 쓸 수 있습니다.');" + " location.href='" + request.getContextPath()
					+ "/board/listArticles.do';" + "</script>");
					return;
				}// 아이디 검사후 비로그인(비정상요청)일경우 팝업과 함꼐 되돌리기
				response.sendRedirect(request.getContextPath() + "/boardjsp/replyForm.jsp"); // dispatch, forward로 인한 이중요청 방지
				return;
			} else if (action.equals("/addReply.do")) {
				session = request.getSession();
				int parentNO = (Integer) session.getAttribute("parentNO");
				String id = (String) session.getAttribute("id"); // 로그인된 유저의 세선에서 id 받아오기
				PrintWriter pw = response.getWriter();
				session.removeAttribute("parentNO");
				Map<String, String> articleMap = upload(request, response);
				String title = articleMap.get("title");
				String content = articleMap.get("content");
				String imageFileName = articleMap.get("imageFileName");
				articleVO.setParentno(parentNO);
				articleVO.setId(id); // 답글 작성자를 세션에서 받아와 추가
				articleVO.setTitle(title);
				articleVO.setContent(content);
				articleVO.setImagefilename(imageFileName);
				int articleNO = boardService.addReply(articleVO);
				if (imageFileName != null && imageFileName.length() != 0) {
					File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
					File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
					destDir.mkdirs();
					FileUtils.moveFileToDirectory(srcFile, destDir, true);
				}

				pw.print("<script>" + "  alert('답글 작성을 완료했습니다.');" + " location.href='" + request.getContextPath()
						+ "/board/viewArticle.do?articleNO="+articleNO+"';" + "</script>");
				return;
			}

			RequestDispatcher dispatch = request.getRequestDispatcher(nextPage);
			dispatch.forward(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String, String> upload(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException { // 이미지 업로드를 위한 함수
		Map<String, String> articleMap = new HashMap<String, String>();
		String encoding = "utf-8";
		File currentDirPath = new File(ARTICLE_IMAGE_REPO);
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setRepository(currentDirPath);
		factory.setSizeThreshold(1024 * 1024);
		ServletFileUpload upload = new ServletFileUpload(factory);
		try {
			List items = upload.parseRequest(request);
			for (int i = 0; i < items.size(); i++) {
				FileItem fileItem = (FileItem) items.get(i);
				if (fileItem.isFormField()) {
					System.out.println(fileItem.getFieldName() + "=" + fileItem.getString(encoding));
					articleMap.put(fileItem.getFieldName(), fileItem.getString(encoding));
				} else {
					System.out.println("파일이름" + fileItem.getFieldName());
					System.out.println("파일사이즈:" + fileItem.getSize() + "bytes");
					if (fileItem.getSize() > 0) {
						int idx = fileItem.getName().lastIndexOf("\\");
						if (idx == -1) {
							idx = fileItem.getName().lastIndexOf("/");
						}

						String fileName = fileItem.getName().substring(idx + 1);
						System.out.println("파일이름:" + fileName);
								articleMap.put(fileItem.getFieldName(), fileName);  

						File uploadFile = new File(currentDirPath + "\\temp\\" + fileName);
						fileItem.write(uploadFile);

					} // end if
				} // end if
			} // end for
		} catch (Exception e) {
			e.printStackTrace();
		}
		return articleMap;
	}
}
