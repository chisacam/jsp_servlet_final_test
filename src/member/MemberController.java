package member;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import member.MemberVO;

@WebServlet("/member/*")
public class MemberController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	MemberDAO memberDAO;

	public void init() throws ServletException {
		memberDAO = new MemberDAO();
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
		String nextPage = null;
		
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html;charset=utf-8");
		String action = request.getPathInfo();
		HttpSession session;
		if (action == null || action.equals("/listMembers.do")) { // root 뒤에 요청하는 주소가 없거나 listMember.do일경우 멤버리스트 반환
			List<MemberVO> membersList = memberDAO.listMembers();
			
			request.setAttribute("membersList", membersList);
			
			nextPage = "/memberjsp/listMembers.jsp"; // 렌더링할 페이지는 listMembers.jsp
		} else if (action.equals("/addMember.do")) {
			String id = request.getParameter("id");
			String pwd = request.getParameter("pwd");
			String name = request.getParameter("name");
			String email = request.getParameter("email");
			
			MemberVO memberVO = new MemberVO(id, pwd, name, email); // id, 비밀번호, 이름, 이메일을 받아서 새 멤버 객체를 생성
			memberDAO.addMember(memberVO); // DAO에 멤버 객체를 넣어 DB에 저장
			
			request.setAttribute("msg", "addMember");
			
			nextPage = "/board/listArticles.do"; // 가입후 메인 페이지로 이동시킴
		} else if (action.equals("/memberForm.do")) { // 단순 jsp 포워딩용
			nextPage = "/memberjsp/memberForm.jsp";
		}else if(action.equals("/modMemberForm.do")){
		     String id=request.getParameter("id");
		     session = request.getSession();
		     String _id = (String)session.getAttribute("id");
			 PrintWriter pw = response.getWriter();
		     if(id.equals(_id)) { // 본인인지 검사
			     MemberVO memInfo = memberDAO.findMember(id); // 수정을 요청할 경우 먼저 기본 값을 폼에 넣어주기 위해 id를 받아서 기존정보를 검색
				    
			     request.setAttribute("memInfo", memInfo); // 기본정보를 응답에 담아 전송
			    
			     nextPage="/memberjsp/modMemberForm.jsp";
		     }else {
		    	 pw.print("<script>" + "  alert('본인 정보만 수정할 수 있습니다.');" + " location.href='" + request.getContextPath() + "/member/listMembers.do" +"';" + "</script>");
		    	 return;
		     }

		}else if(action.equals("/modMember.do")){
		     String id=request.getParameter("id");
		     String pwd=request.getParameter("pwd");
		     String name= request.getParameter("name");
	         String email= request.getParameter("email");
		     
	         MemberVO memberVO = new MemberVO(id, pwd, name, email); // 새로 작성된 정보로 멤버 객체 생성
		     memberDAO.modMember(memberVO); // 새로 생성된 객체로 db 업데이트
		     
		     request.setAttribute("msg", "modified");
		     
		     nextPage="/board/listArticles.do";
		}else if(action.equals("/delMember.do")){
		     String id=request.getParameter("id");
		     session = request.getSession();
		     String _id = (String)session.getAttribute("id");
			 PrintWriter pw = response.getWriter();
			 if(id.equals(_id)) { // 세션의 id를 이용해 본인확인
			     memberDAO.delMember(id);
			     
			     request.setAttribute("msg", "deleted");
			     
			     nextPage="/member/listMembers.do";
			 }
			 else {
		    	 pw.print("<script>" + "  alert('본인만 탈퇴할 수 있습니다.');" + " location.href='" + request.getContextPath() + "/member/listMembers.do" +"';" + "</script>");
		    	 return;
			 }
		}else if(action.equals("/login.do")){
			String id = request.getParameter("id");
			String pw = request.getParameter("pwd");
			session = request.getSession();
			if(memberDAO.loginMember(id, pw)) { // 입력한 id와 패스워드를 db에 검색해 매칭하는 경우만 로그인처리
				
				session.setAttribute("id", id);
				nextPage = "/board/listArticles.do";
			} else {
				PrintWriter pwr = response.getWriter();
				pwr.print("<script>" + "  alert('아이디와 비밀번호가 일치하지 않습니다.');" + " location.href='" + request.getContextPath() + "/memberjsp/login.jsp" +"';" + "</script>");
				return;
			}
			
		}else if(action.equals("/logout.do")){
			session = request.getSession();
			session.removeAttribute("id"); // 로그아웃 요청시 세션에 저장한 id 삭제
			response.sendRedirect(request.getContextPath() + "/board/listArticles.do");
			return;
		}else {
			List<MemberVO> membersList = memberDAO.listMembers();
			
			request.setAttribute("membersList", membersList);
			
			nextPage = "/memberjsp/listMembers.jsp";
		}
		RequestDispatcher dispatch = request.getRequestDispatcher(nextPage);
		dispatch.forward(request, response);
	}
}
