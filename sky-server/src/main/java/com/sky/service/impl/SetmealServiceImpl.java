package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    public void saveWithDish(SetmealDTO setmealDTO) {

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        //向套餐表插入一条数据
        setmealMapper.insert(setmeal);

        //获取insert语句生成的主键值
        Long setmealId = setmeal.getId();

        //向套餐菜品表中插入n条数据
        List<SetmealDish> dishes = setmealDTO.getSetmealDishes();
        if(dishes!=null&&dishes.size()>0){
            dishes.forEach(setmealDish->{
                setmealDish.setSetmealId(setmealId);
            });
            setmealDishMapper.insertBatch(dishes);
        }

    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {

        //根据id查询套餐基本信息
        Setmeal setmeal = setmealMapper.getById(id);

        //根据分类id查询分类信息
        Category category = categoryMapper.getById(setmeal.getCategoryId());

        //根据套餐id批量查询菜品信息
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setCategoryName(category.getName());
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 根据id集合批量删除套餐
     * @param ids
     */
    public void deleteBatch(List<Long> ids) {
        //判断套餐是否能被删除----套餐是否起售中？
        for(Long id:ids){
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus()== StatusConstant.ENABLE){
                //套餐起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //根据套餐id集合批量删除套餐数据
        setmealMapper.deleteByIds(ids);

        //根据套餐id集合批量删除套餐菜品数据
        setmealDishMapper.deleteBySetmealIds(ids);

    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        //修改套餐数据
        setmealMapper.update(setmeal);

        //删除套餐菜品数据
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        //重新插入套餐菜品数据
        List<SetmealDish> dishes = setmealDTO.getSetmealDishes();
        if(dishes!=null&&dishes.size()>0){
            dishes.forEach(setmealDish->{
                setmealDish.setSetmealId(setmeal.getId());
            });
            setmealDishMapper.insertBatch(dishes);
        }
    }

    /**
     * 禁用启用菜品
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {

        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();

        setmealMapper.update(setmeal);
    }
}
