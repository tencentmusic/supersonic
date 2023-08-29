/*
//package com.tencent.supersonic.chat.aspect;
//
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.*;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Aspect
//@Component
//@Slf4j
//public class TimeCostAspect {
//
//    ThreadLocal<Long> startTime = new ThreadLocal<>();
//
//    ThreadLocal<Map<String, Long>> map = new ThreadLocal<>();
//
//    @Pointcut("execution(public * com.tencent.supersonic.chat.mapper.HanlpDictMapper.*(*))")
//    //@Pointcut("execution(* public com.tencent.supersonic.chat.parser.*.*(..))")
//    //@Pointcut("execution(* com.tencent.supersonic.chat.mapper.*Mapper.map(..)) ")
//    //@Pointcut("execution(* com.tencent.supersonic.chat.mapper.HanlpDictMapper.map(..)) ")
//    //@Pointcut("execution(* com.tencent.supersonic.chat.parser.rule.QueryModeParser.*(..)) ")
//    public void point() {
//    }
//
//    @Around("point()")
//    public void doAround(ProceedingJoinPoint joinPoint) throws Throwable {
//        long start = System.currentTimeMillis();
//        try {
//            log.info("切面开始");
//            Object result = joinPoint.proceed();
//            log.info("切面开始");
//            if (result == null) {
//                //如果切到了 没有返回类型的void方法，这里直接返回
//                //return null;
//            }
//            long end = System.currentTimeMillis();
//            log.info("===================");
//            String targetClassName = joinPoint.getSignature().getDeclaringTypeName();
//            String MethodName = joinPoint.getSignature().getName();
//            String typeStr = joinPoint.getSignature().getDeclaringType().toString().split(" ")[0];
//            log.info("类/接口:" + targetClassName + "(" + typeStr + ")");
//            log.info("方法:" + MethodName);
//            Long total = end - start;
//            log.info("耗时: " + total + " ms!");
//            map.get().put(targetClassName + "_" + MethodName, total);
//            //return result;
//        } catch (Throwable e) {
//            long end = System.currentTimeMillis();
//            log.info("====around " + joinPoint + "\tUse time : " + (end - start) + " ms with exception : "
//                    + e.getMessage());
//            throw e;
//        }
//    }
//
////    //对Controller下面的方法执行前进行切入，初始化开始时间
////    @Before(value = "execution(* com.appleyk.controller.*.*(..))")
////    public void beforMehhod(JoinPoint jp) {
////        startTime.set(System.currentTimeMillis());
////    }
////
////    //对Controller下面的方法执行后进行切入，统计方法执行的次数和耗时情况
////    //注意，这里的执行方法统计的数据不止包含Controller下面的方法，也包括环绕切入的所有方法的统计信息
////    @AfterReturning(value = "execution(* com.appleyk.controller.*.*(..))")
////    public void afterMehhod(JoinPoint jp) {
////        long end = System.currentTimeMillis();
////        long total =  end - startTime.get();
////        String methodName = jp.getSignature().getName();
////        log.info("连接点方法为：" + methodName + ",执行总耗时为：" +total+"ms");
////
////        //重新new一个map
////        Map<String, Long> map = new HashMap<>();
//////从map2中将最后的 连接点方法给移除了，替换成最终的，避免连接点方法多次进行叠加计算
////        //由于map2受ThreadLocal的保护，这里不支持remove，因此，需要单开一个map进行数据交接
////        for(Map.Entry<String, Long> entry:map2.get().entrySet()){
////            if(entry.getKey().equals(methodName)){
////                map.put(methodName, total);
////
////            }else{
////                map.put(entry.getKey(), entry.getValue());
////            }
////        }
////
////        for (Map.Entry<String, Long> entry :map1.get().entrySet()) {
////            for(Map.Entry<String, Long> entry2 :map.entrySet()){
////                if(entry.getKey().equals(entry2.getKey())){
////                    System.err.println(entry.getKey()+",被调用次数："+entry.getValue()+",综合耗时："+entry2.getValue()+"ms");
////                }
////            }
////
////        }
////    }
//
//}
*/
