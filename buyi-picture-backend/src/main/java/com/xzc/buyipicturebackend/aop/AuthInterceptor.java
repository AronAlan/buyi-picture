package com.xzc.buyipicturebackend.aop;

import com.xzc.buyipicturebackend.annotation.AuthCheck;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.model.enums.UserRoleEnum;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验AOP
 *
 * @author xuzhichao
 * @since 2024-12-26
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        //接口所需要的权限
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        //不需要权限，直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        //需要权限，校验权限
        //当前登录用户拥有的权限
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum userRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        //没有权限，则拒绝。(但其实默认是user，应该不会是null，只是为了防止特殊情况)
        if (userRole == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //要求管理员权限，但用户没有管理员权限，则拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRole)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        //通过权限校验，放行
        return joinPoint.proceed();
    }
}
